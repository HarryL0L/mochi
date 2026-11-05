// SPDX-License-Identifier: GPL-3.0-or-later
package com.harryl.island

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Debug builds only. Fakes a playing song so the compact pill can be tested without a music app:
 *   adb shell am broadcast -a com.harryl.island.debug.MUSIC -n com.harryl.island/.DebugReceiver
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        IslandBus.post(
            IslandEvent.Music(
                IslandContent.Music(
                    packageName = context.packageName, title = "Renegade", artist = "Axwell /\\ Ingrosso",
                    art = null, playing = true, position = 40_000, duration = 191_000,
                    positionAt = SystemClock.elapsedRealtime(), speed = 1f, accent = 0xFFE0477B.toInt(),
                ),
            ),
        )
    }
}
