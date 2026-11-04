package com.harryl.island

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Quick Settings tile: one tap turns Mochi's taps off (for payment apps that refuse to open while
 * an accessibility service is on) and back on afterwards. The island keeps showing either way.
 */
class TapsTileService : TileService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val turnOn = !Taps.isOn(this)
        when (Taps.set(this, turnOn)) {
            Taps.Result.DONE -> {
                // The system binds or unbinds the service shortly after the setting changes.
                refresh(turnOn)
                handler.postDelayed({ refresh() }, 600)
            }
            Taps.Result.NEEDS_SETUP -> {
                Toast.makeText(this, R.string.tile_needs_setup_toast, Toast.LENGTH_LONG).show()
                launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun refresh(assumeOn: Boolean? = null) {
        val tile = qsTile ?: return
        val on = assumeOn ?: Taps.isOn(this)
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = getString(
                when {
                    on -> R.string.tile_on
                    Taps.canSwitchOn(this) -> R.string.tile_off
                    else -> R.string.tile_off_setup
                },
            )
        }
        tile.updateTile()
    }

    private fun launch(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
