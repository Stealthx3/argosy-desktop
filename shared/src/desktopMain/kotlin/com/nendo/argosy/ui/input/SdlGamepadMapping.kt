package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.core.input.AndroidMotionAxes
import com.nendo.argosy.platform.sdl.Sdl3

/**
 * SDL's positional gamepad buttons and axes expressed as the key codes and motion axes Android's
 * Generic.kl gives a standard pad (BTN_SOUTH is KEYCODE_BUTTON_A, ABS_RX/RY are AXIS_Z/RZ).
 */
object SdlGamepadMapping {
    const val DEVICE_ID_BASE = 100

    private val BUTTON_KEYCODES = mapOf(
        Sdl3.BUTTON_SOUTH to AndroidKeyCodes.KEYCODE_BUTTON_A,
        Sdl3.BUTTON_EAST to AndroidKeyCodes.KEYCODE_BUTTON_B,
        Sdl3.BUTTON_WEST to AndroidKeyCodes.KEYCODE_BUTTON_X,
        Sdl3.BUTTON_NORTH to AndroidKeyCodes.KEYCODE_BUTTON_Y,
        Sdl3.BUTTON_BACK to AndroidKeyCodes.KEYCODE_BUTTON_SELECT,
        Sdl3.BUTTON_GUIDE to AndroidKeyCodes.KEYCODE_BUTTON_MODE,
        Sdl3.BUTTON_START to AndroidKeyCodes.KEYCODE_BUTTON_START,
        Sdl3.BUTTON_LEFT_STICK to AndroidKeyCodes.KEYCODE_BUTTON_THUMBL,
        Sdl3.BUTTON_RIGHT_STICK to AndroidKeyCodes.KEYCODE_BUTTON_THUMBR,
        Sdl3.BUTTON_LEFT_SHOULDER to AndroidKeyCodes.KEYCODE_BUTTON_L1,
        Sdl3.BUTTON_RIGHT_SHOULDER to AndroidKeyCodes.KEYCODE_BUTTON_R1,
        Sdl3.BUTTON_DPAD_UP to AndroidKeyCodes.KEYCODE_DPAD_UP,
        Sdl3.BUTTON_DPAD_DOWN to AndroidKeyCodes.KEYCODE_DPAD_DOWN,
        Sdl3.BUTTON_DPAD_LEFT to AndroidKeyCodes.KEYCODE_DPAD_LEFT,
        Sdl3.BUTTON_DPAD_RIGHT to AndroidKeyCodes.KEYCODE_DPAD_RIGHT,
    )

    private val AXES = mapOf(
        Sdl3.AXIS_LEFTX to AndroidMotionAxes.AXIS_X,
        Sdl3.AXIS_LEFTY to AndroidMotionAxes.AXIS_Y,
        Sdl3.AXIS_RIGHTX to AndroidMotionAxes.AXIS_Z,
        Sdl3.AXIS_RIGHTY to AndroidMotionAxes.AXIS_RZ,
        Sdl3.AXIS_LEFT_TRIGGER to AndroidMotionAxes.AXIS_LTRIGGER,
        Sdl3.AXIS_RIGHT_TRIGGER to AndroidMotionAxes.AXIS_RTRIGGER,
    )

    private val DPAD_BUTTONS = setOf(
        Sdl3.BUTTON_DPAD_UP, Sdl3.BUTTON_DPAD_DOWN, Sdl3.BUTTON_DPAD_LEFT, Sdl3.BUTTON_DPAD_RIGHT,
    )

    fun deviceId(instanceId: Int): Int = DEVICE_ID_BASE + instanceId

    fun keyCode(button: Int): Int? = BUTTON_KEYCODES[button]

    fun isDpad(button: Int): Boolean = button in DPAD_BUTTONS

    fun axis(sdlAxis: Int): Int? = AXES[sdlAxis]

    fun axisValue(sdlAxis: Int, value: Short): Float {
        val scaled = value / Short.MAX_VALUE.toFloat()
        return if (sdlAxis == Sdl3.AXIS_LEFT_TRIGGER || sdlAxis == Sdl3.AXIS_RIGHT_TRIGGER) {
            scaled.coerceIn(0f, 1f)
        } else {
            scaled.coerceIn(-1f, 1f)
        }
    }

    fun controllerNumber(playerIndex: Int): Int = if (playerIndex >= 0) playerIndex + 1 else 0
}
