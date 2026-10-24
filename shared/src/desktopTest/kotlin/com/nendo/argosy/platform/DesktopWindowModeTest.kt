package com.nendo.argosy.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopWindowModeTest {
    @Test
    fun theSettingDecidesWithoutAFlag() {
        assertFalse(DesktopWindowMode.startsFullScreen(setting = false, override = null, gamescope = false))
        assertTrue(DesktopWindowMode.startsFullScreen(setting = true, override = null, gamescope = false))
    }

    @Test
    fun aFlagOverridesTheSetting() {
        assertTrue(DesktopWindowMode.startsFullScreen(setting = false, override = true, gamescope = false))
        assertFalse(DesktopWindowMode.startsFullScreen(setting = true, override = false, gamescope = false))
    }

    @Test
    fun gamescopeIsAlwaysFullScreen() {
        assertTrue(DesktopWindowMode.startsFullScreen(setting = false, override = false, gamescope = true))
        assertTrue(DesktopWindowMode.isGamescope(mapOf("XDG_CURRENT_DESKTOP" to "gamescope")))
        assertTrue(DesktopWindowMode.isGamescope(mapOf("GAMESCOPE_WAYLAND_DISPLAY" to "gamescope-0")))
        assertFalse(DesktopWindowMode.isGamescope(mapOf("XDG_CURRENT_DESKTOP" to "KDE")))
    }

    @Test
    fun flagsParse() {
        assertEquals(true, DesktopWindowMode.overrideFrom(arrayOf("--fullscreen")))
        assertEquals(false, DesktopWindowMode.overrideFrom(arrayOf("--windowed", "--size=800x600")))
        assertNull(DesktopWindowMode.overrideFrom(arrayOf("--key-pipe=/tmp/x")))
    }
}
