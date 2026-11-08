package com.nendo.argosy.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ControllerFamilyTest {
    @Test
    fun vendorIdPicksTheFamily() {
        assertEquals(ControllerFamily.PLAYSTATION, controllerFamily("030000004c050000e60c000011810000", false))
        assertEquals(ControllerFamily.NINTENDO, controllerFamily("030000007e0500000920000011810000", false))
        assertEquals(ControllerFamily.STEAM, controllerFamily("03000000de2800000512000011010000", false))
        assertEquals(ControllerFamily.XBOX, controllerFamily("030000005e040000120b000009050000", false))
    }

    @Test
    fun unknownOrMissingGuidFallsBackToTheLayoutFlag() {
        assertEquals(ControllerFamily.XBOX, controllerFamily(null, false))
        assertEquals(ControllerFamily.NINTENDO, controllerFamily(null, true))
        assertEquals(ControllerFamily.XBOX, controllerFamily("short", false))
    }

    @Test
    fun aChosenLayoutOverridesDetection() {
        val sony = "030000004c050000e60c000011810000"
        assertEquals(ControllerFamily.XBOX, controllerFamily("xbox", sony, false))
        assertEquals(ControllerFamily.STEAM, controllerFamily("steam", sony, false))
        assertEquals(ControllerFamily.NINTENDO, controllerFamily("nintendo", null, false))
        assertEquals(ControllerFamily.PLAYSTATION, controllerFamily("auto", sony, false))
    }
}
