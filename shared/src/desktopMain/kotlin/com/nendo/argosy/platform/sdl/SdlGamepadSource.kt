package com.nendo.argosy.platform.sdl

import com.nendo.argosy.util.Logger
import com.sun.jna.Memory
import com.sun.jna.Pointer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

sealed interface SdlPadEvent {
    val instanceId: Int

    data class Added(
        override val instanceId: Int,
        val name: String,
        val vendorId: Int,
        val productId: Int,
        val descriptor: String,
        val playerIndex: Int,
    ) : SdlPadEvent

    data class Removed(override val instanceId: Int) : SdlPadEvent

    data class Button(override val instanceId: Int, val button: Int, val down: Boolean) : SdlPadEvent

    data class Axis(override val instanceId: Int, val axis: Int, val value: Short) : SdlPadEvent
}

/**
 * Owns SDL's gamepad subsystem on one thread: SDL is initialised, pumped and every gamepad opened
 * and closed there, and [onEvent] is called on that thread for each gamepad event.
 */
class SdlGamepadSource(
    private val sdl: Sdl3Library,
    private val onEvent: (SdlPadEvent) -> Unit,
) {
    private val open = HashMap<Int, Pointer>()
    private val ready = CountDownLatch(1)

    @Volatile
    private var running = false

    @Volatile
    var initialized = false
        private set

    fun start() {
        running = true
        Thread(::run, "sdl-gamepad").apply { isDaemon = true }.start()
    }

    fun awaitReady(timeoutMs: Long): Boolean = ready.await(timeoutMs, TimeUnit.MILLISECONDS) && initialized

    fun stop() {
        running = false
    }

    private fun run() {
        sdl.SDL_SetHint(Sdl3.HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, "1")
        if (sdl.SDL_Init(Sdl3.INIT_GAMEPAD) == FALSE) {
            Logger.warn(TAG, "SDL_Init failed: ${sdl.SDL_GetError()}")
            ready.countDown()
            return
        }
        initialized = true
        ready.countDown()
        val event = Memory(Sdl3.EVENT_SIZE)
        while (running) {
            if (sdl.SDL_WaitEventTimeout(event, WAIT_MS) == FALSE) continue
            translate(event)?.let(onEvent)
        }
        open.values.forEach(sdl::SDL_CloseGamepad)
        open.clear()
        sdl.SDL_Quit()
    }

    private fun translate(event: Pointer): SdlPadEvent? {
        val which = event.getInt(Sdl3.EVENT_WHICH_OFFSET)
        return when (event.getInt(0)) {
            Sdl3.EVENT_GAMEPAD_ADDED -> opened(which)
            Sdl3.EVENT_GAMEPAD_REMOVED -> {
                open.remove(which)?.let(sdl::SDL_CloseGamepad) ?: return null
                SdlPadEvent.Removed(which)
            }
            Sdl3.EVENT_GAMEPAD_BUTTON_DOWN, Sdl3.EVENT_GAMEPAD_BUTTON_UP -> SdlPadEvent.Button(
                which,
                event.getByte(Sdl3.EVENT_BUTTON_OFFSET).toInt() and 0xff,
                event.getByte(Sdl3.EVENT_DOWN_OFFSET) != FALSE,
            )
            Sdl3.EVENT_GAMEPAD_AXIS_MOTION -> SdlPadEvent.Axis(
                which,
                event.getByte(Sdl3.EVENT_AXIS_OFFSET).toInt() and 0xff,
                event.getShort(Sdl3.EVENT_AXIS_VALUE_OFFSET),
            )
            else -> null
        }
    }

    private fun opened(instanceId: Int): SdlPadEvent? {
        if (instanceId in open) return null
        val gamepad = sdl.SDL_OpenGamepad(instanceId)
        if (gamepad == null) {
            Logger.warn(TAG, "SDL_OpenGamepad($instanceId) failed: ${sdl.SDL_GetError()}")
            return null
        }
        open[instanceId] = gamepad
        val location = sdl.SDL_GetGamepadSerial(gamepad)?.takeIf { it.isNotBlank() }
            ?: sdl.SDL_GetGamepadPath(gamepad).orEmpty()
        return SdlPadEvent.Added(
            instanceId = instanceId,
            name = sdl.SDL_GetGamepadName(gamepad) ?: "Gamepad",
            vendorId = sdl.SDL_GetGamepadVendor(gamepad).toInt() and 0xffff,
            productId = sdl.SDL_GetGamepadProduct(gamepad).toInt() and 0xffff,
            descriptor = "${sdl.SDL_GetGamepadGUIDForID(instanceId).hex()}:$location",
            playerIndex = sdl.SDL_GetGamepadPlayerIndex(gamepad),
        )
    }

    private companion object {
        const val TAG = "SdlGamepadSource"
        const val WAIT_MS = 100
        const val FALSE: Byte = 0
    }
}
