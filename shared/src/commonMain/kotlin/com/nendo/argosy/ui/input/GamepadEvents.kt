package com.nendo.argosy.ui.input

import android.view.KeyEvent

data class GamepadInput(
    val event: GamepadEvent,
    val isRepeat: Boolean = false,
    val signature: InputSignature? = null
)

sealed interface GamepadEvent {
    data object Up : GamepadEvent
    data object Down : GamepadEvent
    data object Left : GamepadEvent
    data object Right : GamepadEvent
    data object Confirm : GamepadEvent
    data object Back : GamepadEvent
    data object SecondaryAction : GamepadEvent
    data object ContextMenu : GamepadEvent
    data object PrevSection : GamepadEvent
    data object NextSection : GamepadEvent
    data object PrevTrigger : GamepadEvent
    data object NextTrigger : GamepadEvent
    data object Menu : GamepadEvent
    data object Select : GamepadEvent
    data object LeftStickClick : GamepadEvent
    data object RightStickClick : GamepadEvent
    data object RightStickUp : GamepadEvent
    data object RightStickDown : GamepadEvent
    data object RightStickLeft : GamepadEvent
    data object RightStickRight : GamepadEvent
    data object Home : GamepadEvent
    data object LongConfirm : GamepadEvent
    data object LongSelect : GamepadEvent
}

fun mapKeycodeToGamepadEvent(
    keyCode: Int,
    swapAB: Boolean = false,
    swapXY: Boolean = false,
    swapStartSelect: Boolean = false
): GamepadEvent? = when (keyCode) {
    KeyEvent.KEYCODE_DPAD_UP -> GamepadEvent.Up
    KeyEvent.KEYCODE_DPAD_DOWN -> GamepadEvent.Down
    KeyEvent.KEYCODE_DPAD_LEFT -> GamepadEvent.Left
    KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadEvent.Right

    KeyEvent.KEYCODE_BUTTON_A -> if (swapAB) GamepadEvent.Back else GamepadEvent.Confirm
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_DPAD_CENTER -> GamepadEvent.Confirm

    KeyEvent.KEYCODE_BUTTON_B -> if (swapAB) GamepadEvent.Confirm else GamepadEvent.Back
    KeyEvent.KEYCODE_ESCAPE,
    KeyEvent.KEYCODE_BACK -> GamepadEvent.Back

    KeyEvent.KEYCODE_BUTTON_X -> if (swapXY) GamepadEvent.SecondaryAction else GamepadEvent.ContextMenu
    KeyEvent.KEYCODE_BUTTON_Y -> if (swapXY) GamepadEvent.ContextMenu else GamepadEvent.SecondaryAction

    KeyEvent.KEYCODE_BUTTON_L1 -> GamepadEvent.PrevSection
    KeyEvent.KEYCODE_BUTTON_R1 -> GamepadEvent.NextSection

    KeyEvent.KEYCODE_BUTTON_L2 -> GamepadEvent.PrevTrigger
    KeyEvent.KEYCODE_BUTTON_R2 -> GamepadEvent.NextTrigger

    KeyEvent.KEYCODE_BUTTON_START -> if (swapStartSelect) GamepadEvent.Select else GamepadEvent.Menu
    KeyEvent.KEYCODE_BUTTON_SELECT -> if (swapStartSelect) GamepadEvent.Menu else GamepadEvent.Select

    KeyEvent.KEYCODE_BUTTON_THUMBL -> GamepadEvent.LeftStickClick
    KeyEvent.KEYCODE_BUTTON_THUMBR -> GamepadEvent.RightStickClick
    KeyEvent.KEYCODE_HOME,
    KeyEvent.KEYCODE_BUTTON_MODE -> GamepadEvent.Home

    else -> null
}

fun gamepadEventToKeyCode(event: GamepadEvent): Int? = when (event) {
    GamepadEvent.Up -> KeyEvent.KEYCODE_DPAD_UP
    GamepadEvent.Down -> KeyEvent.KEYCODE_DPAD_DOWN
    GamepadEvent.Left -> KeyEvent.KEYCODE_DPAD_LEFT
    GamepadEvent.Right -> KeyEvent.KEYCODE_DPAD_RIGHT
    else -> null
}
