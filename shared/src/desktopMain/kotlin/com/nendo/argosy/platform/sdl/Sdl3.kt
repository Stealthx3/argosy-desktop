package com.nendo.argosy.platform.sdl

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Suppress("FunctionName")
interface Sdl3Library : Library {
    fun SDL_SetHint(name: String, value: String): Byte
    fun SDL_SetHintWithPriority(name: String, value: String, priority: Int): Byte
    fun SDL_Init(flags: Int): Byte
    fun SDL_Quit()
    fun SDL_GetError(): String?
    fun SDL_WaitEventTimeout(event: Pointer, timeoutMs: Int): Byte
    fun SDL_OpenGamepad(instanceId: Int): Pointer?
    fun SDL_CloseGamepad(gamepad: Pointer)
    fun SDL_GetGamepadName(gamepad: Pointer): String?
    fun SDL_GetGamepadPath(gamepad: Pointer): String?
    fun SDL_GetGamepadSerial(gamepad: Pointer): String?
    fun SDL_GetGamepadVendor(gamepad: Pointer): Short
    fun SDL_GetGamepadProduct(gamepad: Pointer): Short
    fun SDL_GetGamepadPlayerIndex(gamepad: Pointer): Int
    fun SDL_GetGamepadType(gamepad: Pointer): Int
    fun SDL_GetGamepadSteamHandle(gamepad: Pointer): Long
    fun SDL_RumbleGamepad(gamepad: Pointer, lowFrequency: Short, highFrequency: Short, durationMs: Int): Byte
    fun SDL_GetGamepadGUIDForID(instanceId: Int): SdlGuid.ByValue
    fun SDL_AttachVirtualJoystick(desc: Pointer): Int
    fun SDL_DetachVirtualJoystick(instanceId: Int): Byte
    fun SDL_OpenJoystick(instanceId: Int): Pointer?
    fun SDL_CloseJoystick(joystick: Pointer)
    fun SDL_SetJoystickVirtualButton(joystick: Pointer, button: Int, down: Byte): Byte
    fun SDL_SetJoystickVirtualAxis(joystick: Pointer, axis: Int, value: Short): Byte
}

@Structure.FieldOrder("data")
open class SdlGuid : Structure() {
    @JvmField
    var data = ByteArray(16)

    class ByValue : SdlGuid(), Structure.ByValue

    fun hex(): String = data.joinToString("") { "%02x".format(it) }
}

object Sdl3 {
    const val INIT_GAMEPAD = 0x00002000
    const val HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS = "SDL_JOYSTICK_ALLOW_BACKGROUND_EVENTS"
    const val HINT_ALLOW_STEAM_VIRTUAL_GAMEPAD = "SDL_GAMECONTROLLER_ALLOW_STEAM_VIRTUAL_GAMEPAD"
    const val HINT_GAMECONTROLLER_IGNORE_DEVICES = "SDL_GAMECONTROLLER_IGNORE_DEVICES"
    const val HINT_JOYSTICK_HIDAPI_STEAM = "SDL_JOYSTICK_HIDAPI_STEAM"
    const val HINT_PRIORITY_OVERRIDE = 2

    const val VENDOR_VALVE = 0x28de
    const val PRODUCT_STEAM_VIRTUAL_GAMEPAD = 0x11ff

    const val EVENT_GAMEPAD_AXIS_MOTION = 0x650
    const val EVENT_GAMEPAD_BUTTON_DOWN = 0x651
    const val EVENT_GAMEPAD_BUTTON_UP = 0x652
    const val EVENT_GAMEPAD_ADDED = 0x653
    const val EVENT_GAMEPAD_REMOVED = 0x654

    const val EVENT_SIZE = 128L
    const val EVENT_WHICH_OFFSET = 16L
    const val EVENT_BUTTON_OFFSET = 20L
    const val EVENT_DOWN_OFFSET = 21L
    const val EVENT_AXIS_OFFSET = 20L
    const val EVENT_AXIS_VALUE_OFFSET = 24L

    const val BUTTON_SOUTH = 0
    const val BUTTON_EAST = 1
    const val BUTTON_WEST = 2
    const val BUTTON_NORTH = 3
    const val BUTTON_BACK = 4
    const val BUTTON_GUIDE = 5
    const val BUTTON_START = 6
    const val BUTTON_LEFT_STICK = 7
    const val BUTTON_RIGHT_STICK = 8
    const val BUTTON_LEFT_SHOULDER = 9
    const val BUTTON_RIGHT_SHOULDER = 10
    const val BUTTON_DPAD_UP = 11
    const val BUTTON_DPAD_DOWN = 12
    const val BUTTON_DPAD_LEFT = 13
    const val BUTTON_DPAD_RIGHT = 14
    const val STANDARD_BUTTON_COUNT = 15

    const val AXIS_LEFTX = 0
    const val AXIS_LEFTY = 1
    const val AXIS_RIGHTX = 2
    const val AXIS_RIGHTY = 3
    const val AXIS_LEFT_TRIGGER = 4
    const val AXIS_RIGHT_TRIGGER = 5
    const val AXIS_COUNT = 6

    const val JOYSTICK_TYPE_GAMEPAD = 1

    val BUTTON_NAMES: Map<String, Int> = mapOf(
        "SOUTH" to BUTTON_SOUTH,
        "EAST" to BUTTON_EAST,
        "WEST" to BUTTON_WEST,
        "NORTH" to BUTTON_NORTH,
        "BACK" to BUTTON_BACK,
        "GUIDE" to BUTTON_GUIDE,
        "START" to BUTTON_START,
        "LEFT_STICK" to BUTTON_LEFT_STICK,
        "RIGHT_STICK" to BUTTON_RIGHT_STICK,
        "LEFT_SHOULDER" to BUTTON_LEFT_SHOULDER,
        "RIGHT_SHOULDER" to BUTTON_RIGHT_SHOULDER,
        "DPAD_UP" to BUTTON_DPAD_UP,
        "DPAD_DOWN" to BUTTON_DPAD_DOWN,
        "DPAD_LEFT" to BUTTON_DPAD_LEFT,
        "DPAD_RIGHT" to BUTTON_DPAD_RIGHT,
    )

    val AXIS_NAMES: Map<String, Int> = mapOf(
        "LEFTX" to AXIS_LEFTX,
        "LEFTY" to AXIS_LEFTY,
        "RIGHTX" to AXIS_RIGHTX,
        "RIGHTY" to AXIS_RIGHTY,
        "LEFT_TRIGGER" to AXIS_LEFT_TRIGGER,
        "RIGHT_TRIGGER" to AXIS_RIGHT_TRIGGER,
    )

    private val library: Sdl3Library? by lazy {
        LIBRARY_NAMES.firstNotNullOfOrNull { name ->
            runCatching { Native.load(name, Sdl3Library::class.java) }.getOrNull()
        }
    }

    fun load(): Sdl3Library? = library

    private val LIBRARY_NAMES = listOf("SDL3", "libSDL3.so.0")
}
