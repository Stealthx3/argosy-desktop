package com.nendo.argosy.ui.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.platform.PlatformKeyEvent
import com.nendo.argosy.platform.SOURCE_KEYBOARD
import com.nendo.argosy.ui.audio.AmbientAudioManager
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

/**
 * Desktop counterpart of ArgosyActivity.dispatchKeyEvent: translates a window key event into the
 * key code Android's Generic.kl layout gives the same physical key, then hands it to upstream's
 * GamepadInputHandler, so a keyboard behaves exactly as it does on Android.
 */
@Singleton
class DesktopKeyDispatcher @Inject constructor(
    private val gamepadInputHandler: GamepadInputHandler,
    private val ambientAudioManager: AmbientAudioManager,
) {
    private val held = mutableMapOf<Int, HeldKey>()

    fun dispatch(event: ComposeKeyEvent): Boolean {
        val keyCode = KEYBOARD_KEYCODES[event.key] ?: return false
        val action = when (event.type) {
            KeyEventType.KeyDown -> AndroidKeyCodes.ACTION_DOWN
            KeyEventType.KeyUp -> AndroidKeyCodes.ACTION_UP
            else -> return false
        }
        val now = System.currentTimeMillis()
        val repeatCount: Int
        val downTime: Long
        if (action == AndroidKeyCodes.ACTION_DOWN) {
            val previous = held[keyCode]
            val current = previous?.let { it.copy(repeatCount = it.repeatCount + 1) } ?: HeldKey(now, 0)
            held[keyCode] = current
            repeatCount = current.repeatCount
            downTime = current.downTime
        } else {
            downTime = held.remove(keyCode)?.downTime ?: now
            repeatCount = 0
        }
        val platformEvent = PlatformKeyEvent(
            deviceId = KEYBOARD_DEVICE_ID,
            source = SOURCE_KEYBOARD,
            eventTime = now,
            eventKeyCode = keyCode,
            eventAction = action,
            eventRepeatCount = repeatCount,
            eventDownTime = downTime,
        )
        if (action == AndroidKeyCodes.ACTION_DOWN) {
            ambientAudioManager.resumeFromSuspend()
        }
        if (gamepadInputHandler.handleKeyEvent(platformEvent)) {
            return true
        }
        if (keyCode == AndroidKeyCodes.KEYCODE_HOME || keyCode == AndroidKeyCodes.KEYCODE_BUTTON_MODE) {
            if (action == AndroidKeyCodes.ACTION_DOWN) gamepadInputHandler.emitHomeEvent()
            return true
        }
        return false
    }

    fun releaseAll() {
        held.clear()
    }

    private data class HeldKey(val downTime: Long, val repeatCount: Int)

    private companion object {
        const val KEYBOARD_DEVICE_ID = 1

        val KEYBOARD_KEYCODES: Map<Key, Int> = buildMap {
            val letters = listOf(
                Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
                Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z,
            )
            letters.forEachIndexed { index, key -> put(key, AndroidKeyCodes.KEYCODE_A + index) }
            val digits = listOf(
                Key.Zero, Key.One, Key.Two, Key.Three, Key.Four,
                Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine,
            )
            digits.forEachIndexed { index, key -> put(key, AndroidKeyCodes.KEYCODE_0 + index) }
            val numPad = listOf(
                Key.NumPad0, Key.NumPad1, Key.NumPad2, Key.NumPad3, Key.NumPad4,
                Key.NumPad5, Key.NumPad6, Key.NumPad7, Key.NumPad8, Key.NumPad9,
            )
            numPad.forEachIndexed { index, key -> put(key, AndroidKeyCodes.KEYCODE_NUMPAD_0 + index) }
            val functionKeys = listOf(
                Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6,
                Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12,
            )
            functionKeys.forEachIndexed { index, key -> put(key, AndroidKeyCodes.KEYCODE_F1 + index) }
            put(Key.DirectionUp, AndroidKeyCodes.KEYCODE_DPAD_UP)
            put(Key.DirectionDown, AndroidKeyCodes.KEYCODE_DPAD_DOWN)
            put(Key.DirectionLeft, AndroidKeyCodes.KEYCODE_DPAD_LEFT)
            put(Key.DirectionRight, AndroidKeyCodes.KEYCODE_DPAD_RIGHT)
            put(Key.Enter, AndroidKeyCodes.KEYCODE_ENTER)
            put(Key.NumPadEnter, AndroidKeyCodes.KEYCODE_NUMPAD_ENTER)
            put(Key.Escape, AndroidKeyCodes.KEYCODE_ESCAPE)
            put(Key.Backspace, AndroidKeyCodes.KEYCODE_DEL)
            put(Key.Delete, AndroidKeyCodes.KEYCODE_FORWARD_DEL)
            put(Key.Tab, AndroidKeyCodes.KEYCODE_TAB)
            put(Key.Spacebar, AndroidKeyCodes.KEYCODE_SPACE)
            put(Key.MoveHome, AndroidKeyCodes.KEYCODE_MOVE_HOME)
            put(Key.MoveEnd, AndroidKeyCodes.KEYCODE_MOVE_END)
            put(Key.PageUp, AndroidKeyCodes.KEYCODE_PAGE_UP)
            put(Key.PageDown, AndroidKeyCodes.KEYCODE_PAGE_DOWN)
            put(Key.Insert, AndroidKeyCodes.KEYCODE_INSERT)
            put(Key.ShiftLeft, AndroidKeyCodes.KEYCODE_SHIFT_LEFT)
            put(Key.ShiftRight, AndroidKeyCodes.KEYCODE_SHIFT_RIGHT)
            put(Key.CtrlLeft, AndroidKeyCodes.KEYCODE_CTRL_LEFT)
            put(Key.CtrlRight, AndroidKeyCodes.KEYCODE_CTRL_RIGHT)
            put(Key.AltLeft, AndroidKeyCodes.KEYCODE_ALT_LEFT)
            put(Key.AltRight, AndroidKeyCodes.KEYCODE_ALT_RIGHT)
            put(Key.MetaLeft, AndroidKeyCodes.KEYCODE_META_LEFT)
            put(Key.MetaRight, AndroidKeyCodes.KEYCODE_META_RIGHT)
            put(Key.CapsLock, AndroidKeyCodes.KEYCODE_CAPS_LOCK)
            put(Key.Grave, AndroidKeyCodes.KEYCODE_GRAVE)
            put(Key.Minus, AndroidKeyCodes.KEYCODE_MINUS)
            put(Key.Equals, AndroidKeyCodes.KEYCODE_EQUALS)
            put(Key.LeftBracket, AndroidKeyCodes.KEYCODE_LEFT_BRACKET)
            put(Key.RightBracket, AndroidKeyCodes.KEYCODE_RIGHT_BRACKET)
            put(Key.Backslash, AndroidKeyCodes.KEYCODE_BACKSLASH)
            put(Key.Semicolon, AndroidKeyCodes.KEYCODE_SEMICOLON)
            put(Key.Apostrophe, AndroidKeyCodes.KEYCODE_APOSTROPHE)
            put(Key.Comma, AndroidKeyCodes.KEYCODE_COMMA)
            put(Key.Period, AndroidKeyCodes.KEYCODE_PERIOD)
            put(Key.Slash, AndroidKeyCodes.KEYCODE_SLASH)
        }
    }
}
