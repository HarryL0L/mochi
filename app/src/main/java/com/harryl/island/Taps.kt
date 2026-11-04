package com.harryl.island

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.quicksettings.TileService

/**
 * Turns Mochi's accessibility (and so its taps) on or off.
 *
 * Off always works: the service may switch itself off with disableSelf(). On needs
 * WRITE_SECURE_SETTINGS, which only a one-time adb grant can give:
 *   adb shell pm grant com.harryl.island android.permission.WRITE_SECURE_SETTINGS
 * Only Mochi's own entry is ever added or removed; other services are left untouched.
 */
object Taps {
    enum class Result { DONE, NEEDS_SETUP }

    fun grantCommand(context: Context) =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    fun isOn(context: Context) = IslandAccessibilityService.isEnabled(context)

    fun canSwitchOn(context: Context) =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun set(context: Context, on: Boolean): Result {
        val pkg = context.packageName
        val cls = IslandAccessibilityService::class.java.name
        if (canSwitchOn(context)) {
            val cr = context.contentResolver
            val list = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val next = if (on) A11yList.add(list, pkg, cls) else A11yList.remove(list, pkg, cls)
            runCatching {
                Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, next)
                if (on) Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            }.onSuccess { return Result.DONE }
        }
        if (!on) {
            val service = IslandAccessibilityService.instance
            if (service != null) {
                service.disableSelf()
                return Result.DONE
            }
            if (!isOn(context)) return Result.DONE
        }
        return Result.NEEDS_SETUP
    }

    /** Asks the system to refresh the tile, e.g. after the service turned on or off elsewhere. */
    fun refreshTile(context: Context) {
        runCatching { TileService.requestListeningState(context, ComponentName(context, TapsTileService::class.java)) }
    }
}
