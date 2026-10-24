package com.nendo.argosy.platform

import com.nendo.argosy.data.preferences.DisplayProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DisplayProfileTest {
    @Test
    fun storedProfileWinsOverDetection() {
        assertEquals(DisplayProfile.TV, DisplayProfile.effective(DisplayProfile.TV, DisplayProfile.HANDHELD))
        assertEquals(DisplayProfile.HANDHELD, DisplayProfile.effective(null, DisplayProfile.HANDHELD))
        assertEquals(DisplayProfile.DESKTOP, DisplayProfile.effective(null, null))
    }

    @Test
    fun safeAreaFollowsTheProfileUntilSet() {
        assertEquals(3, DisplayProfile.effectiveSafeArea(null, DisplayProfile.TV))
        assertEquals(0, DisplayProfile.effectiveSafeArea(null, DisplayProfile.DESKTOP))
        assertEquals(1, DisplayProfile.effectiveSafeArea(1, DisplayProfile.TV))
        assertEquals(5, DisplayProfile.effectiveSafeArea(9, DisplayProfile.TV))
    }

    @Test
    fun tvStartsAtCouchScale() {
        assertEquals(125, DisplayProfile.TV.defaultUiScale)
        assertEquals(100, DisplayProfile.DESKTOP.defaultUiScale)
        assertEquals(false, DisplayProfile.DESKTOP.hidesIdleCursor)
        assertEquals(true, DisplayProfile.HANDHELD.hidesIdleCursor)
    }

    @Test
    fun detectionClassifiesHandheldsTvsAndDesktops() {
        val d = DisplayProfileDetection
        assertEquals(DisplayProfile.HANDHELD, d.classify("Jupiter", "Valve", gamescope = true, largestDiagonalInches = 7.4))
        assertEquals(DisplayProfile.HANDHELD, d.classify("RC71L", "ASUSTeK COMPUTER INC.", gamescope = false, largestDiagonalInches = 7.0))
        assertEquals(DisplayProfile.TV, d.classify("B650", "Micro-Star", gamescope = false, largestDiagonalInches = 54.6))
        assertEquals(DisplayProfile.TV, d.classify("B650", "Micro-Star", gamescope = true, largestDiagonalInches = 27.0))
        assertEquals(DisplayProfile.DESKTOP, d.classify("B650", "Micro-Star", gamescope = false, largestDiagonalInches = 34.0))
        assertEquals(DisplayProfile.DESKTOP, d.classify(null, null, gamescope = false, largestDiagonalInches = null))
    }

    @Test
    fun edidDiagonalComesFromItsSizeBytes() {
        val edid = ByteArray(128).also { it[21] = 121; it[22] = 68 }
        assertEquals(54.6, DisplayProfileDetection.diagonalInches(edid)!!, 0.1)
        assertNull(DisplayProfileDetection.diagonalInches(ByteArray(128)))
    }
}
