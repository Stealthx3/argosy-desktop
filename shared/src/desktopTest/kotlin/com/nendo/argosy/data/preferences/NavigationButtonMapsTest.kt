package com.nendo.argosy.data.preferences

import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.ui.input.GamepadEvent
import com.nendo.argosy.ui.input.mapKeycodeToGamepadEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationButtonMapsTest {
    private val pad = "03000000de2800000512000011010000"
    private val a = AndroidKeyCodes.KEYCODE_BUTTON_A
    private val b = AndroidKeyCodes.KEYCODE_BUTTON_B
    private val y = AndroidKeyCodes.KEYCODE_BUTTON_Y

    @Test
    fun emptyMapsTranslateToTheSameKeyCode() {
        assertEquals(a, NavigationButtonMaps().translate(pad, a))
    }

    @Test
    fun assigningAButtonSwapsItWithTheDisplacedOne() {
        val maps = NavigationButtonMaps().assign(pad, physicalKeyCode = y, mappedKeyCode = a)
        assertEquals(a, maps.translate(pad, y))
        assertEquals(y, maps.translate(pad, a))
        assertEquals(b, maps.translate(pad, b))
        assertEquals(y, maps.physicalFor(pad, a))
    }

    @Test
    fun repeatedAssignmentsStayAPermutation() {
        val maps = NavigationButtonMaps()
            .assign(pad, y, a)
            .assign(pad, b, y)
            .assign(pad, a, b)
        val codes = listOf(a, b, y)
        assertEquals(codes.toSet(), codes.map { maps.translate(pad, it) }.toSet())
    }

    @Test
    fun assigningBackToItselfRestoresTheDefault() {
        val maps = NavigationButtonMaps().assign(pad, y, a).assign(pad, a, a)
        assertTrue(maps.byController.isEmpty())
    }

    @Test
    fun swapsApplyOnTopOfTheRemap() {
        val maps = NavigationButtonMaps().assign(pad, y, a)
        assertEquals(GamepadEvent.Back, mapKeycodeToGamepadEvent(maps.translate(pad, y), swapAB = true))
        assertEquals(GamepadEvent.Confirm, mapKeycodeToGamepadEvent(maps.translate(pad, y)))
    }

    @Test
    fun controllersAreIndependentAndResetClearsOne() {
        val maps = NavigationButtonMaps()
            .assign(pad, y, a)
            .assign(NavigationButtonMaps.KEYBOARD, AndroidKeyCodes.KEYCODE_SPACE, AndroidKeyCodes.KEYCODE_ENTER)
        assertEquals(y, maps.translate(NavigationButtonMaps.KEYBOARD, y))
        val reset = maps.reset(pad)
        assertEquals(y, reset.translate(pad, y))
        assertEquals(AndroidKeyCodes.KEYCODE_ENTER, reset.translate(NavigationButtonMaps.KEYBOARD, AndroidKeyCodes.KEYCODE_SPACE))
    }

    @Test
    fun encodeDecodeRoundTrips() {
        val maps = NavigationButtonMaps()
            .assign(pad, y, a)
            .assign(NavigationButtonMaps.KEYBOARD, AndroidKeyCodes.KEYCODE_SPACE, AndroidKeyCodes.KEYCODE_ENTER)
        assertEquals(maps, NavigationButtonMaps.decode(maps.encode()))
        assertEquals("", NavigationButtonMaps().encode())
    }

    @Test
    fun decodeDropsMalformedAndNonPermutationEntries() {
        val decoded = NavigationButtonMaps.decode("bad;$pad=96:100,100:96;other=96:97,97:98;x=1:q")
        assertEquals(setOf(pad), decoded.byController.keys)
        assertEquals(NavigationButtonMaps(), NavigationButtonMaps.decode(null))
    }
}
