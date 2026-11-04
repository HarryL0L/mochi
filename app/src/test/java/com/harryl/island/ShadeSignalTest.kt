// SPDX-License-Identifier: GPL-3.0-or-later
package com.harryl.island

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadeSignalTest {
    private val me = "com.harryl.island"
    private fun open(pkg: String?, cls: String? = null, gone: Boolean = false, current: Boolean = false) =
        ShadeSignal.isShadeOpen(pkg, cls, gone, me, current)

    @Test
    fun systemUiWindowMeansShadeOrLockScreen() {
        assertTrue(open("com.android.systemui", "android.widget.FrameLayout"))
    }

    @Test
    fun appComingToFrontClosesIt() {
        assertFalse(open("in.org.npci.upiapp", current = true))
    }

    @Test
    fun paneDisappearingClosesIt() {
        assertFalse(open("com.android.systemui", "android.widget.FrameLayout", gone = true, current = true))
    }

    @Test
    fun volumeDialogAndOwnWindowsChangeNothing() {
        assertFalse(open("com.android.systemui", "com.android.systemui.volume.VolumeDialogImpl", current = false))
        assertTrue(open("com.android.systemui", "com.android.systemui.volume.VolumeDialogImpl", current = true))
        assertTrue(open(me, current = true))
        assertTrue(open(null, current = true))
    }
}
