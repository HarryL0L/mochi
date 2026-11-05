// SPDX-License-Identifier: GPL-3.0-or-later
package com.harryl.island

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * What the island is doing and why, shown under About → Island status and included in
 * Copy details, so a missing or misbehaving island can be explained without a computer.
 */
object IslandDebug {
    private val log = ArrayDeque<String>()
    private val time = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Volatile
    var status: String = "Not running"

    @Volatile
    var host: String = "None"

    @Synchronized
    fun add(message: String) {
        log.addLast(time.format(Date()) + "  " + message)
        while (log.size > 40) log.removeFirst()
    }

    @Synchronized
    fun recent(n: Int = 15): List<String> = log.toList().takeLast(n)
}
