package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.core.input.AndroidMotionAxes
import com.nendo.argosy.platform.PlatformInputDevice
import com.nendo.argosy.platform.PlatformInputDevices
import com.nendo.argosy.platform.PlatformMotionEvent
import com.nendo.argosy.platform.SOURCE_JOYSTICK
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.keyCode
import com.nendo.argosy.platform.sdl.Sdl3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopGamepadMappingTest {
    @Test
    fun faceButtonsArePositionalLikeGenericKl() {
        assertEquals(AndroidKeyCodes.KEYCODE_BUTTON_A, SdlGamepadMapping.keyCode(Sdl3.BUTTON_SOUTH))
        assertEquals(AndroidKeyCodes.KEYCODE_BUTTON_B, SdlGamepadMapping.keyCode(Sdl3.BUTTON_EAST))
        assertEquals(AndroidKeyCodes.KEYCODE_BUTTON_X, SdlGamepadMapping.keyCode(Sdl3.BUTTON_WEST))
        assertEquals(AndroidKeyCodes.KEYCODE_BUTTON_Y, SdlGamepadMapping.keyCode(Sdl3.BUTTON_NORTH))
        assertEquals(AndroidKeyCodes.KEYCODE_BUTTON_SELECT, SdlGamepadMapping.keyCode(Sdl3.BUTTON_BACK))
        assertEquals(AndroidKeyCodes.KEYCODE_DPAD_DOWN, SdlGamepadMapping.keyCode(Sdl3.BUTTON_DPAD_DOWN))
        assertNull(SdlGamepadMapping.keyCode(Sdl3.STANDARD_BUTTON_COUNT))
    }

    @Test
    fun axesScaleToAndroidRanges() {
        assertEquals(AndroidMotionAxes.AXIS_RZ, SdlGamepadMapping.axis(Sdl3.AXIS_RIGHTY))
        assertEquals(1f, SdlGamepadMapping.axisValue(Sdl3.AXIS_LEFTX, Short.MAX_VALUE))
        assertEquals(-1f, SdlGamepadMapping.axisValue(Sdl3.AXIS_LEFTX, Short.MIN_VALUE))
        assertEquals(0f, SdlGamepadMapping.axisValue(Sdl3.AXIS_LEFT_TRIGGER, Short.MIN_VALUE))
    }

    @Test
    fun triggerPastHalfEmitsL2DownThenUp() {
        val deviceId = 4242
        PlatformInputDevices.connected(PlatformInputDevice(deviceId, "Pad", 1, 2, "d"))
        try {
            val emitter = TriggerAxisKeyEmitter()
            val pressed = emitter.emit(motion(deviceId, 0.9f))
            assertEquals(listOf(AndroidKeyCodes.KEYCODE_BUTTON_L2), pressed.map { it.keyCode })
            assertEquals(AndroidKeyCodes.ACTION_DOWN, pressed.single().action)
            assertEquals(emptyList(), emitter.emit(motion(deviceId, 0.8f)))
            assertEquals(AndroidKeyCodes.ACTION_UP, emitter.emit(motion(deviceId, 0f)).single().action)
        } finally {
            PlatformInputDevices.disconnected(deviceId)
        }
    }

    private fun motion(deviceId: Int, trigger: Float) = PlatformMotionEvent(
        deviceId,
        SOURCE_JOYSTICK,
        0L,
        AndroidMotionAxes.ACTION_MOVE,
        mapOf(AndroidMotionAxes.AXIS_LTRIGGER to trigger),
    )
}
