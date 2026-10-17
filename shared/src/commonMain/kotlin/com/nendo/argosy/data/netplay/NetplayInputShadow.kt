package com.nendo.argosy.data.netplay

import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.device
import com.nendo.argosy.platform.deviceId
import com.nendo.argosy.platform.downTime
import com.nendo.argosy.platform.eventTime
import com.nendo.argosy.platform.isFromSource
import com.nendo.argosy.platform.keyCode
import com.nendo.argosy.platform.metaState
import com.nendo.argosy.platform.repeatCount
import com.nendo.argosy.platform.scanCode
import com.nendo.argosy.platform.source
import com.nendo.argosy.core.input.AndroidKeyCodes
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class NetplayInputShadow {

    private val lock = ReentrantReadWriteLock()
    private val bitmasks = IntArray(MAX_PORTS)

    fun onKeyDown(port: Int, retroKey: Int) {
        if (!isValidPort(port) || !isValidKey(retroKey)) return
        lock.write {
            bitmasks[port] = bitmasks[port] or (1 shl retroKey)
        }
    }

    fun onKeyUp(port: Int, retroKey: Int) {
        if (!isValidPort(port) || !isValidKey(retroKey)) return
        lock.write {
            bitmasks[port] = bitmasks[port] and (1 shl retroKey).inv()
        }
    }

    fun sample(port: Int): Int {
        if (!isValidPort(port)) return 0
        return lock.read { bitmasks[port] }
    }

    fun clear(port: Int) {
        if (!isValidPort(port)) return
        lock.write { bitmasks[port] = 0 }
    }

    fun onAndroidKeyEvent(port: Int, action: Int, androidKeyCode: Int) {
        val retroKey = androidKeyToRetroId(androidKeyCode) ?: return
        when (action) {
            AndroidKeyCodes.ACTION_DOWN -> onKeyDown(port, retroKey)
            AndroidKeyCodes.ACTION_UP -> onKeyUp(port, retroKey)
        }
    }

    private fun isValidPort(port: Int): Boolean = port in 0 until MAX_PORTS

    private fun isValidKey(key: Int): Boolean = key in 0..RETRO_DEVICE_ID_JOYPAD_R3

    companion object {
        const val MAX_PORTS = 4

        const val RETRO_DEVICE_ID_JOYPAD_B = 0
        const val RETRO_DEVICE_ID_JOYPAD_Y = 1
        const val RETRO_DEVICE_ID_JOYPAD_SELECT = 2
        const val RETRO_DEVICE_ID_JOYPAD_START = 3
        const val RETRO_DEVICE_ID_JOYPAD_UP = 4
        const val RETRO_DEVICE_ID_JOYPAD_DOWN = 5
        const val RETRO_DEVICE_ID_JOYPAD_LEFT = 6
        const val RETRO_DEVICE_ID_JOYPAD_RIGHT = 7
        const val RETRO_DEVICE_ID_JOYPAD_A = 8
        const val RETRO_DEVICE_ID_JOYPAD_X = 9
        const val RETRO_DEVICE_ID_JOYPAD_L = 10
        const val RETRO_DEVICE_ID_JOYPAD_R = 11
        const val RETRO_DEVICE_ID_JOYPAD_L2 = 12
        const val RETRO_DEVICE_ID_JOYPAD_R2 = 13
        const val RETRO_DEVICE_ID_JOYPAD_L3 = 14
        const val RETRO_DEVICE_ID_JOYPAD_R3 = 15

        fun androidKeyToRetroId(keyCode: Int): Int? = when (keyCode) {
            AndroidKeyCodes.KEYCODE_BUTTON_START -> RETRO_DEVICE_ID_JOYPAD_START
            AndroidKeyCodes.KEYCODE_BUTTON_SELECT -> RETRO_DEVICE_ID_JOYPAD_SELECT
            AndroidKeyCodes.KEYCODE_BUTTON_A -> RETRO_DEVICE_ID_JOYPAD_A
            AndroidKeyCodes.KEYCODE_BUTTON_B -> RETRO_DEVICE_ID_JOYPAD_B
            AndroidKeyCodes.KEYCODE_BUTTON_X -> RETRO_DEVICE_ID_JOYPAD_X
            AndroidKeyCodes.KEYCODE_BUTTON_Y -> RETRO_DEVICE_ID_JOYPAD_Y
            AndroidKeyCodes.KEYCODE_BUTTON_L1 -> RETRO_DEVICE_ID_JOYPAD_L
            AndroidKeyCodes.KEYCODE_BUTTON_L2 -> RETRO_DEVICE_ID_JOYPAD_L2
            AndroidKeyCodes.KEYCODE_BUTTON_R1 -> RETRO_DEVICE_ID_JOYPAD_R
            AndroidKeyCodes.KEYCODE_BUTTON_R2 -> RETRO_DEVICE_ID_JOYPAD_R2
            AndroidKeyCodes.KEYCODE_BUTTON_THUMBL -> RETRO_DEVICE_ID_JOYPAD_L3
            AndroidKeyCodes.KEYCODE_BUTTON_THUMBR -> RETRO_DEVICE_ID_JOYPAD_R3
            AndroidKeyCodes.KEYCODE_DPAD_UP -> RETRO_DEVICE_ID_JOYPAD_UP
            AndroidKeyCodes.KEYCODE_DPAD_DOWN -> RETRO_DEVICE_ID_JOYPAD_DOWN
            AndroidKeyCodes.KEYCODE_DPAD_LEFT -> RETRO_DEVICE_ID_JOYPAD_LEFT
            AndroidKeyCodes.KEYCODE_DPAD_RIGHT -> RETRO_DEVICE_ID_JOYPAD_RIGHT
            else -> null
        }
    }
}
