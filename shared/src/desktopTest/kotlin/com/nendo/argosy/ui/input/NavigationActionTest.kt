package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.data.preferences.NavigationButtonMaps
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationActionTest {
    private val pad = "03000000de2800000512000011010000"

    @Test
    fun everyActionResolvesToACodeThatProducesItUnderEverySwap() {
        val flags = listOf(false, true)
        for (keyboard in flags) for (ab in flags) for (xy in flags) for (ss in flags) {
            for (action in NavigationAction.entries) {
                val code = action.standardCode(keyboard, ab, xy, ss)
                assertEquals(action.event, mapKeycodeToGamepadEvent(code, ab, xy, ss), "$action keyboard=$keyboard")
            }
        }
    }

    @Test
    fun swapsMoveConfirmToTheOtherFaceButton() {
        assertEquals(AndroidKeyCodes.KEYCODE_BUTTON_A, NavigationAction.CONFIRM.standardCode(false, false, false, false))
        assertEquals(AndroidKeyCodes.KEYCODE_BUTTON_B, NavigationAction.CONFIRM.standardCode(false, true, false, false))
    }

    @Test
    fun keyboardConfirmAndBackAreEnterAndEscape() {
        assertEquals(AndroidKeyCodes.KEYCODE_ENTER, NavigationAction.CONFIRM.standardCode(true, true, false, false))
        assertEquals(AndroidKeyCodes.KEYCODE_ESCAPE, NavigationAction.BACK.standardCode(true, false, false, false))
    }

    @Test
    fun assigningAPressedButtonRoutesItToTheAction() {
        val standard = NavigationAction.CONTEXT.standardCode(false, false, false, false)
        val maps = NavigationButtonMaps().assign(pad, AndroidKeyCodes.KEYCODE_BUTTON_L1, standard)
        val event = mapKeycodeToGamepadEvent(maps.translate(pad, AndroidKeyCodes.KEYCODE_BUTTON_L1))
        assertEquals(GamepadEvent.ContextMenu, event)
        assertEquals(AndroidKeyCodes.KEYCODE_BUTTON_L1, maps.physicalFor(pad, standard))
    }
}
