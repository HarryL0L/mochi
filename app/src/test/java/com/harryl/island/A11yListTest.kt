// SPDX-License-Identifier: GPL-3.0-or-later
package com.harryl.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class A11yListTest {
    private val pkg = "com.harryl.island"
    private val cls = "com.harryl.island.IslandAccessibilityService"
    private val other = "com.mojang.minecraftpe/com.mojang.minecraftpe.AccessibilityService"

    @Test
    fun recognisesShortAndLongForms() {
        assertTrue(A11yList.contains("$pkg/.IslandAccessibilityService", pkg, cls))
        assertTrue(A11yList.contains("$other:$pkg/$cls", pkg, cls))
        assertFalse(A11yList.contains(other, pkg, cls))
        assertFalse(A11yList.contains(null, pkg, cls))
    }

    @Test
    fun addKeepsOtherServicesAndNeverDuplicates() {
        assertEquals("$other:$pkg/$cls", A11yList.add(other, pkg, cls))
        assertEquals("$pkg/$cls", A11yList.add("", pkg, cls))
        assertEquals("$pkg/.IslandAccessibilityService", A11yList.add("$pkg/.IslandAccessibilityService", pkg, cls))
    }

    @Test
    fun removeOnlyTouchesMochi() {
        assertEquals(other, A11yList.remove("$other:$pkg/.IslandAccessibilityService", pkg, cls))
        assertEquals("", A11yList.remove("$pkg/$cls", pkg, cls))
        assertEquals(other, A11yList.remove("$other::", pkg, cls))
    }
}
