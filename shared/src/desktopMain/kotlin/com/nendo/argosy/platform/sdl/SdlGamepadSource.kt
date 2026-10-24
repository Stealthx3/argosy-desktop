package com.nendo.argosy.platform.sdl

import com.nendo.argosy.data.preferences.ControllerInputMode
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
        val gamepadType: Int,
        val steamVirtual: Boolean,
    ) : SdlPadEvent

    data class Removed(override val instanceId: Int) : SdlPadEvent

    data class Button(override val instanceId: Int, val button: Int, val down: Boolean) : SdlPadEvent

    data class Axis(override val instanceId: Int, val axis: Int, val value: Short) : SdlPadEvent
}

/**
 * Owns SDL's gamepad subsystem on one thread: SDL is initialised, pumped and every gamepad opened
 * and closed there, and [onEvent] is called on that thread for each gamepad event. [rumble] may be
 * called from any thread. [mode] is STEAM_INPUT (Steam's virtual gamepads, the devices Steam hides
 * stay hidden) or SDL3 (physical pads only, Valve hardware through SDL's HIDAPI driver).
 */
class SdlGamepadSource(
    private val sdl: Sdl3Library,
    val mode: ControllerInputMode,
    private val onEvent: (SdlPadEvent) -> Unit,
) {
    private val open = HashMap<Int, Pointer>()
    private val ready = CountDownLatch(1)

    @Volatile
    private var running = false

    @Volatile
    var initialized = false
        private set

    @Volatile
    private var lastActive: Int? = null

    private var thread: Thread? = null

    fun start() {
        running = true
        thread = Thread(::run, "sdl-gamepad").apply { isDaemon = true }.also { it.start() }
    }

    fun stopAndJoin(timeoutMs: Long) {
        running = false
        thread?.join(timeoutMs)
    }

    fun awaitReady(timeoutMs: Long): Boolean = ready.await(timeoutMs, TimeUnit.MILLISECONDS) && initialized

    fun stop() {
        running = false
    }

    /**
     * Rumbles the gamepad that last sent input (or any open one) at [low] and [high] motor
     * strength, 0 to 65535, for [durationMs]. False when no gamepad is open or it cannot rumble.
     */
    fun rumble(low: Int, high: Int, durationMs: Int): Boolean = synchronized(open) {
        val gamepad = lastActive?.let(open::get) ?: open.values.firstOrNull() ?: return false
        sdl.SDL_RumbleGamepad(gamepad, low.toShort(), high.toShort(), durationMs) != FALSE
    }

    private fun run() {
        configureHints()
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
            val padEvent = translate(event) ?: continue
            if (padEvent is SdlPadEvent.Button || padEvent is SdlPadEvent.Axis) lastActive = padEvent.instanceId
            onEvent(padEvent)
        }
        synchronized(open) {
            open.values.forEach(sdl::SDL_CloseGamepad)
            open.clear()
        }
        sdl.SDL_Quit()
    }

    private fun configureHints() {
        sdl.SDL_SetHint(Sdl3.HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, "1")
        val steamInput = mode == ControllerInputMode.STEAM_INPUT
        override(Sdl3.HINT_ALLOW_STEAM_VIRTUAL_GAMEPAD, if (steamInput) "1" else "0")
        override(Sdl3.HINT_JOYSTICK_HIDAPI_STEAM, if (steamInput) "0" else "1")
        if (!steamInput) override(Sdl3.HINT_GAMECONTROLLER_IGNORE_DEVICES, "")
    }

    private fun override(name: String, value: String) {
        if (sdl.SDL_SetHintWithPriority(name, value, Sdl3.HINT_PRIORITY_OVERRIDE) == FALSE) {
            Logger.warn(TAG, "SDL hint $name=$value refused: ${sdl.SDL_GetError()}")
        }
    }

    private fun translate(event: Pointer): SdlPadEvent? {
        val which = event.getInt(Sdl3.EVENT_WHICH_OFFSET)
        return when (event.getInt(0)) {
            Sdl3.EVENT_GAMEPAD_ADDED -> opened(which)
            Sdl3.EVENT_GAMEPAD_REMOVED -> {
                synchronized(open) { open.remove(which)?.let(sdl::SDL_CloseGamepad) } ?: return null
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
        val vendorId = sdl.SDL_GetGamepadVendor(gamepad).toInt() and 0xffff
        val productId = sdl.SDL_GetGamepadProduct(gamepad).toInt() and 0xffff
        val steamVirtual = sdl.SDL_GetGamepadSteamHandle(gamepad) != 0L ||
            (vendorId == Sdl3.VENDOR_VALVE && productId == Sdl3.PRODUCT_STEAM_VIRTUAL_GAMEPAD)
        if (steamVirtual && mode != ControllerInputMode.STEAM_INPUT) {
            Logger.info(TAG, "Steam virtual gamepad ${sdl.SDL_GetGamepadName(gamepad)} ignored in $mode mode")
            sdl.SDL_CloseGamepad(gamepad)
            return null
        }
        synchronized(open) { open[instanceId] = gamepad }
        val location = sdl.SDL_GetGamepadSerial(gamepad)?.takeIf { it.isNotBlank() }
            ?: sdl.SDL_GetGamepadPath(gamepad).orEmpty()
        return SdlPadEvent.Added(
            instanceId = instanceId,
            name = sdl.SDL_GetGamepadName(gamepad) ?: "Gamepad",
            vendorId = vendorId,
            productId = productId,
            descriptor = "${sdl.SDL_GetGamepadGUIDForID(instanceId).hex()}:$location",
            playerIndex = sdl.SDL_GetGamepadPlayerIndex(gamepad),
            gamepadType = sdl.SDL_GetGamepadType(gamepad),
            steamVirtual = steamVirtual,
        )
    }

    private companion object {
        const val TAG = "SdlGamepadSource"
        const val WAIT_MS = 100
        const val FALSE: Byte = 0
    }
}
