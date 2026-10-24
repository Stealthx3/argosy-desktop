package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.data.preferences.NavigationButtonMaps
import com.nendo.argosy.res.ArgosyR
import com.nendo.argosy.ui.screens.settings.sections.buttonMappingKeyLabelRes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ButtonGlyphRemapTest {
    private val pad = "03000000de2800000512000011010000"
    private val a = AndroidKeyCodes.KEYCODE_BUTTON_A
    private val x = AndroidKeyCodes.KEYCODE_BUTTON_X

    @Test
    fun withoutAControllerEveryCodeShowsItself() {
        val maps = NavigationButtonMaps().assign(pad, x, a)
        val remap = ButtonGlyphRemap(controller = null, maps = maps)
        assertEquals(a, remap.physicalFor(a))
    }

    @Test
    fun aRemappedConfirmShowsThePhysicalButtonThatProducesIt() {
        val maps = NavigationButtonMaps().assign(pad, x, a)
        val remap = ButtonGlyphRemap(controller = pad, maps = maps)
        assertEquals(x, remap.physicalFor(a))
        assertEquals(a, remap.physicalFor(x))
    }

    @Test
    fun anotherControllersMapDoesNotChangeTheGlyphs() {
        val maps = NavigationButtonMaps().assign("other", x, a)
        assertEquals(a, ButtonGlyphRemap(controller = pad, maps = maps).physicalFor(a))
    }

    @Test
    fun directionsReadAsDpadOnAControllerAndArrowsOnTheKeyboard() {
        val up = AndroidKeyCodes.KEYCODE_DPAD_UP
        assertEquals(ArgosyR.string.settings_button_mapping_key_dpad_up, buttonMappingKeyLabelRes(up, keyboard = false))
        assertEquals(ArgosyR.string.settings_button_mapping_key_arrow_up, buttonMappingKeyLabelRes(up, keyboard = true))
        assertEquals(ArgosyR.string.settings_button_mapping_key_enter, buttonMappingKeyLabelRes(AndroidKeyCodes.KEYCODE_ENTER, keyboard = true))
        assertNull(buttonMappingKeyLabelRes(a, keyboard = false))
    }
}
