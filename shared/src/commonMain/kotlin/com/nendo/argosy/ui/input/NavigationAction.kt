package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AndroidKeyCodes as KeyEvent

/**
 * A navigation action a button can be mapped to. The standard code is the key code that
 * [mapKeycodeToGamepadEvent] turns into the action under the current swaps; on the keyboard
 * Confirm and Back are Enter and Escape and the other actions have no key until one is mapped.
 */
enum class NavigationAction(val event: GamepadEvent) {
    CONFIRM(GamepadEvent.Confirm),
    BACK(GamepadEvent.Back),
    CONTEXT(GamepadEvent.ContextMenu),
    SECONDARY(GamepadEvent.SecondaryAction),
    MENU(GamepadEvent.Menu),
    SELECT(GamepadEvent.Select),
    PREV_SECTION(GamepadEvent.PrevSection),
    NEXT_SECTION(GamepadEvent.NextSection),
    PREV_TRIGGER(GamepadEvent.PrevTrigger),
    NEXT_TRIGGER(GamepadEvent.NextTrigger),
    LEFT_STICK_CLICK(GamepadEvent.LeftStickClick),
    RIGHT_STICK_CLICK(GamepadEvent.RightStickClick),
    UP(GamepadEvent.Up),
    DOWN(GamepadEvent.Down),
    LEFT(GamepadEvent.Left),
    RIGHT(GamepadEvent.Right);

    fun standardCode(keyboard: Boolean, swapAB: Boolean, swapXY: Boolean, swapStartSelect: Boolean): Int =
        (if (keyboard) KEYBOARD_CANDIDATES else CONTROLLER_CANDIDATES)
            .first { mapKeycodeToGamepadEvent(it, swapAB, swapXY, swapStartSelect) == event }

    companion object {
        private val SHARED_CANDIDATES = listOf(
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
        )
        private val CONTROLLER_CANDIDATES =
            listOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B) + SHARED_CANDIDATES
        private val KEYBOARD_CANDIDATES = listOf(KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_ESCAPE) + SHARED_CANDIDATES

        val GAMEPAD_BUTTON_CODES: Set<Int> = setOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR, KeyEvent.KEYCODE_BUTTON_MODE,
        )
    }
}
