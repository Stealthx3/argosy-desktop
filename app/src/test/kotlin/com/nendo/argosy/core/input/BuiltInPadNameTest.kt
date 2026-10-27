package com.nendo.argosy.core.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInPadNameTest {

    @Test
    fun `a pad named for the device brand is built in`() {
        assertTrue(ControllerDetector.padNamedForDevice("Retroid Pocket Controller", "Retroid Pocket Nova"))
    }

    @Test
    fun `an external pad on the same device is not`() {
        assertFalse(ControllerDetector.padNamedForDevice("Xbox Wireless Controller", "Retroid Pocket Nova"))
        assertFalse(ControllerDetector.padNamedForDevice(null, "Retroid Pocket Nova"))
    }

    @Test
    fun `a short or missing model never matches`() {
        assertFalse(ControllerDetector.padNamedForDevice("RP Controller", "RP"))
        assertFalse(ControllerDetector.padNamedForDevice("Controller", null))
    }
}
