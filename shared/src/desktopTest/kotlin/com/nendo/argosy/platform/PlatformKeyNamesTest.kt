package com.nendo.argosy.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformKeyNamesTest {
    @Test
    fun namesComeFromTheKeyCodeTable() {
        assertEquals("KEYCODE_BUTTON_A", keyCodeToString(96))
        assertEquals("KEYCODE_DPAD_UP", keyCodeToString(19))
        assertEquals("100000", keyCodeToString(100000))
    }
}
