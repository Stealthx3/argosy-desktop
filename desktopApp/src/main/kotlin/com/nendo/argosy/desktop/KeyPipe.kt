package com.nendo.argosy.desktop

import com.nendo.argosy.platform.sdl.Sdl3
import com.nendo.argosy.platform.sdl.SdlVirtualPad
import com.nendo.argosy.ui.input.DesktopGamepadDispatcher
import java.awt.Component
import java.awt.Container
import java.awt.Window
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.SwingUtilities

/**
 * Development aid for driving the window from scripts (`--key-pipe=<fifo>`): each line read from
 * the pipe names an AWT virtual key (`ENTER`, `DOWN`, `ESCAPE`, ...), which is pressed and released
 * on the window's own component. A directly dispatched event is delivered to its source component,
 * so this works without the window holding OS focus. A line `PAD:<button>` presses a button of an
 * SDL virtual gamepad (`PAD:SOUTH`, `PAD:DPAD_DOWN`) and `PAD:<axis>=<value>` holds an axis
 * (`PAD:LEFTY=32767`, back to rest with `=0`), through the app's real SDL gamepad path. A line
 * `TYPE:<text>` types the text as it stands (case kept), one press, typed and release per character.
 */
class KeyPipe(private val pipe: File, private val gamepads: DesktopGamepadDispatcher) {
    @Volatile
    private var window: Window? = null

    private var pad: SdlVirtualPad? = null
    private var padGeneration = -1

    fun attach(target: Window) {
        window = target
    }

    fun start() {
        Thread({
            while (true) {
                pipe.bufferedReader().useLines { lines -> lines.forEach(::press) }
            }
        }, "key-pipe").apply { isDaemon = true }.start()
    }

    private fun press(line: String) {
        if (line.startsWith(TYPE_PREFIX)) {
            line.removePrefix(TYPE_PREFIX).forEach(::type)
            return
        }
        val name = line.trim().uppercase()
        if (name.isEmpty()) return
        if (name.startsWith(PAD_PREFIX)) {
            padInput(name.removePrefix(PAD_PREFIX))
            return
        }
        val keyCode = runCatching { KeyEvent::class.java.getField("VK_$name").getInt(null) }.getOrNull()
        if (keyCode == null) {
            System.err.println("key-pipe: unknown key $name")
            return
        }
        send(KeyEvent.KEY_PRESSED, keyCode)
        Thread.sleep(HOLD_MS)
        send(KeyEvent.KEY_RELEASED, keyCode)
    }

    private fun type(char: Char) {
        val keyCode = KeyEvent.getExtendedKeyCodeForChar(char.code)
        send(KeyEvent.KEY_PRESSED, keyCode, char)
        send(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, char)
        send(KeyEvent.KEY_RELEASED, keyCode, char)
        Thread.sleep(TYPE_GAP_MS)
    }

    private fun padInput(command: String) {
        val target = virtualPad() ?: return
        val axisName = command.substringBefore('=', "")
        if (axisName.isNotEmpty()) {
            val axis = Sdl3.AXIS_NAMES[axisName]
            val value = command.substringAfter('=').toIntOrNull()?.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            if (axis == null || value == null) {
                System.err.println("key-pipe: bad pad axis $command")
                return
            }
            target.setAxis(axis, value.toShort())
            return
        }
        val button = Sdl3.BUTTON_NAMES[command]
        if (button == null) {
            System.err.println("key-pipe: unknown pad button $command")
            return
        }
        target.setButton(button, true)
        Thread.sleep(HOLD_MS)
        target.setButton(button, false)
    }

    private fun virtualPad(): SdlVirtualPad? {
        pad?.takeIf { padGeneration == gamepads.sourceGeneration }?.let { return it }
        if (!gamepads.awaitReady(PAD_READY_MS)) {
            System.err.println("key-pipe: SDL gamepad input is not running")
            return null
        }
        val rumbleLog: (Int, Int) -> Unit = { low, high -> System.err.println("key-pipe: pad rumble low=$low high=$high") }
        return SdlVirtualPad.attach(VIRTUAL_PAD_NAME, rumbleLog).also {
            if (it == null) System.err.println("key-pipe: SDL virtual gamepad could not be attached")
            pad = it
            padGeneration = gamepads.sourceGeneration
            Thread.sleep(PAD_SETTLE_MS)
        }
    }

    private fun send(id: Int, keyCode: Int, char: Char = KeyEvent.CHAR_UNDEFINED) {
        SwingUtilities.invokeAndWait {
            val target = window?.let { it.mostRecentFocusOwner ?: deepestFocusable(it) } ?: return@invokeAndWait
            target.dispatchEvent(
                KeyEvent(target, id, System.currentTimeMillis(), 0, keyCode, char),
            )
        }
    }

    private fun deepestFocusable(component: Component): Component? {
        if (component is Container) {
            component.components.forEach { child -> deepestFocusable(child)?.let { return it } }
        }
        return component.takeIf { it.isFocusable && it.keyListeners.isNotEmpty() }
    }

    private companion object {
        const val HOLD_MS = 80L
        const val PAD_PREFIX = "PAD:"
        const val TYPE_PREFIX = "TYPE:"
        const val TYPE_GAP_MS = 20L
        const val PAD_READY_MS = 5_000L
        const val PAD_SETTLE_MS = 300L
        const val VIRTUAL_PAD_NAME = "Argosy Virtual Pad"
    }
}
