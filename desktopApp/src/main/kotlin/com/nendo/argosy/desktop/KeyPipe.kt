package com.nendo.argosy.desktop

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
 * so this works without the window holding OS focus.
 */
class KeyPipe(private val pipe: File) {
    @Volatile
    private var window: Window? = null

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
        val name = line.trim().uppercase()
        if (name.isEmpty()) return
        val keyCode = runCatching { KeyEvent::class.java.getField("VK_$name").getInt(null) }.getOrNull()
        if (keyCode == null) {
            System.err.println("key-pipe: unknown key $name")
            return
        }
        send(KeyEvent.KEY_PRESSED, keyCode)
        Thread.sleep(HOLD_MS)
        send(KeyEvent.KEY_RELEASED, keyCode)
    }

    private fun send(id: Int, keyCode: Int) {
        SwingUtilities.invokeAndWait {
            val target = window?.let { it.mostRecentFocusOwner ?: deepestFocusable(it) } ?: return@invokeAndWait
            target.dispatchEvent(
                KeyEvent(target, id, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED),
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
    }
}
