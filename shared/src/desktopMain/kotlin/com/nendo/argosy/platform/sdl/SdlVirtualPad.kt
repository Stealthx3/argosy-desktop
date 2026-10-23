package com.nendo.argosy.platform.sdl

import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Memory
import com.sun.jna.Pointer

/**
 * SDL_VirtualJoystickDesc.Rumble: motor strengths 0 to 65535 as unsigned 16-bit values.
 */
fun interface SdlRumbleCallback : Callback {
    fun invoke(userdata: Pointer?, lowFrequency: Short, highFrequency: Short): Byte
}

/**
 * An SDL virtual gamepad with the fifteen standard buttons and six axes, for driving the app's
 * gamepad path without hardware. Its events arrive through the running [SdlGamepadSource], and
 * rumble sent to it reaches the optional onRumble listener as motor strengths 0 to 65535.
 */
class SdlVirtualPad private constructor(
    private val sdl: Sdl3Library,
    val instanceId: Int,
    private val joystick: Pointer,
    private val retained: List<Any>,
) {
    fun setButton(button: Int, down: Boolean): Boolean =
        sdl.SDL_SetJoystickVirtualButton(joystick, button, if (down) 1 else 0) != FALSE

    fun setAxis(axis: Int, value: Short): Boolean =
        sdl.SDL_SetJoystickVirtualAxis(joystick, axis, value) != FALSE

    fun detach() {
        sdl.SDL_CloseJoystick(joystick)
        sdl.SDL_DetachVirtualJoystick(instanceId)
    }

    companion object {
        private const val FALSE: Byte = 0
        private const val DESC_SIZE = 136L
        private const val TYPE_OFFSET = 4L
        private const val VENDOR_OFFSET = 8L
        private const val PRODUCT_OFFSET = 10L
        private const val NAXES_OFFSET = 12L
        private const val NBUTTONS_OFFSET = 14L
        private const val BUTTON_MASK_OFFSET = 28L
        private const val AXIS_MASK_OFFSET = 32L
        private const val NAME_OFFSET = 40L
        private const val RUMBLE_OFFSET = 88L
        private const val TRUE: Byte = 1

        fun attach(name: String, onRumble: ((Int, Int) -> Unit)? = null): SdlVirtualPad? =
            Sdl3.load()?.let { attach(it, name, onRumble = onRumble) }

        fun attach(
            sdl: Sdl3Library,
            name: String,
            vendorId: Int = 0,
            productId: Int = 0,
            onRumble: ((Int, Int) -> Unit)? = null,
        ): SdlVirtualPad? {
            val nameMemory = Memory(name.toByteArray().size + 1L).apply { setString(0, name) }
            val rumble = onRumble?.let { listener ->
                SdlRumbleCallback { _, low, high ->
                    listener(low.toInt() and 0xffff, high.toInt() and 0xffff)
                    TRUE
                }
            }
            val desc = Memory(DESC_SIZE).apply {
                clear()
                setInt(0, DESC_SIZE.toInt())
                setShort(TYPE_OFFSET, Sdl3.JOYSTICK_TYPE_GAMEPAD.toShort())
                setShort(VENDOR_OFFSET, vendorId.toShort())
                setShort(PRODUCT_OFFSET, productId.toShort())
                setShort(NAXES_OFFSET, Sdl3.AXIS_COUNT.toShort())
                setShort(NBUTTONS_OFFSET, Sdl3.STANDARD_BUTTON_COUNT.toShort())
                setInt(BUTTON_MASK_OFFSET, (1 shl Sdl3.STANDARD_BUTTON_COUNT) - 1)
                setInt(AXIS_MASK_OFFSET, (1 shl Sdl3.AXIS_COUNT) - 1)
                setPointer(NAME_OFFSET, nameMemory)
                rumble?.let { setPointer(RUMBLE_OFFSET, CallbackReference.getFunctionPointer(it)) }
            }
            val instanceId = sdl.SDL_AttachVirtualJoystick(desc)
            if (instanceId == 0) return null
            val joystick = sdl.SDL_OpenJoystick(instanceId) ?: run {
                sdl.SDL_DetachVirtualJoystick(instanceId)
                return null
            }
            return SdlVirtualPad(sdl, instanceId, joystick, listOfNotNull(desc, nameMemory, rumble))
        }
    }
}
