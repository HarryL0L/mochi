// SPDX-License-Identifier: GPL-3.0-or-later
package com.harryl.island

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Normal app overlays sit *below* the status bar, so the status bar swallows every tap on the
 * island. An accessibility overlay sits above it, which is what makes tap-to-expand, media buttons
 * and swipe-to-dismiss work.
 *
 * This service declares no screen-content access at all (see res/xml/accessibility_config.xml).
 * It only receives window-state events, and reads nothing from them but which app or system
 * window came to the front, to hide the island while the notification shade or lock screen is up.
 */
class IslandAccessibilityService : AccessibilityService() {

    private lateinit var prefs: Prefs
    private var shadeOpen = false

    override fun onServiceConnected() {
        prefs = Prefs(this)
        instance = this
        Taps.refreshTile(this)
        if (!prefs.enabled) return
        if (IslandService.running) IslandBus.post(IslandEvent.HostChanged) else IslandService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!IslandService.running) return
        val open = ShadeSignal.isShadeOpen(
            packageName = event.packageName?.toString(),
            className = event.className?.toString(),
            paneDisappeared = event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED != 0,
            ownPackage = packageName,
            current = shadeOpen,
        )
        // Re-report "open" every time: the island service may have timed the last one out.
        if (open != shadeOpen || open) {
            shadeOpen = open
            IslandBus.post(IslandEvent.Shade(open))
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        if (instance !== this) return
        instance = null
        if (shadeOpen) IslandBus.post(IslandEvent.Shade(false))
        IslandBus.post(IslandEvent.HostChanged)
        Taps.refreshTile(this)
    }

    companion object {
        @Volatile
        var instance: IslandAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            val me = ComponentName(context, IslandAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}

/**
 * Works out whether the shade or lock screen is in front from window-state events alone.
 * Pure logic, unit tested.
 */
object ShadeSignal {
    private const val SYSTEM_UI = "com.android.systemui"

    fun isShadeOpen(
        packageName: String?,
        className: String?,
        paneDisappeared: Boolean,
        ownPackage: String,
        current: Boolean,
    ): Boolean {
        val pkg = packageName ?: return current
        if (pkg == ownPackage) return current
        if (pkg != SYSTEM_UI) return false // an app came to the front
        if (paneDisappeared) return false // the shade's pane closed
        val cls = className.orEmpty().lowercase()
        // Small system UI such as the volume dialog or a toast doesn't cover the island's area.
        if ("volume" in cls || "toast" in cls) return current
        return true
    }
}
