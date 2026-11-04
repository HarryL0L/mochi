// SPDX-License-Identifier: GPL-3.0-or-later
package com.harryl.island

/**
 * Edits Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, a colon-separated list of
 * "package/class" entries where the class may be written short (".Service"). Pure, unit tested.
 */
object A11yList {

    private fun normalise(entry: String): Pair<String, String>? {
        val slash = entry.indexOf('/')
        if (slash <= 0 || slash == entry.length - 1) return null
        val pkg = entry.substring(0, slash)
        val cls = entry.substring(slash + 1).let { if (it.startsWith(".")) pkg + it else it }
        return pkg to cls
    }

    private fun entries(list: String?): List<String> =
        list.orEmpty().split(':').map { it.trim() }.filter { it.isNotEmpty() }

    fun contains(list: String?, pkg: String, cls: String): Boolean =
        entries(list).any { normalise(it) == (pkg to cls) }

    fun add(list: String?, pkg: String, cls: String): String {
        val current = entries(list)
        if (current.any { normalise(it) == (pkg to cls) }) return current.joinToString(":")
        return (current + "$pkg/$cls").joinToString(":")
    }

    fun remove(list: String?, pkg: String, cls: String): String =
        entries(list).filterNot { normalise(it) == (pkg to cls) }.joinToString(":")
}
