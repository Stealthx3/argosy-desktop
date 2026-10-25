package com.nendo.argosy.core.input

import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ControllerPointerGuard"

/**
 * Keeps a game controller's touchpad pointer from moving input focus to one launcher window while
 * a play session is live. Such a pointer reaching the window while another activity is in front
 * makes the window not touchable for [PASS_THROUGH_MS].
 */
class ControllerPointerGuard(
    private val windowName: String,
    private val scope: CoroutineScope,
    private val sessionLive: () -> Boolean,
    private val setWindowTouchable: (Boolean) -> Unit,
    private val isControllerPointer: (MotionEvent) -> Boolean = ::isControllerTouchpadEvent
) {
    private var inFront = false
    private var passThrough: Job? = null

    fun consumes(event: MotionEvent): Boolean {
        if (!sessionLive() || !isControllerPointer(event)) return false
        if (!inFront && passThrough == null) passPointersThrough()
        return true
    }

    fun onInFrontChanged(isInFront: Boolean) {
        inFront = isInFront
        if (isInFront) release()
    }

    fun release() {
        val job = passThrough ?: return
        job.cancel()
        endPassThrough()
    }

    private fun passPointersThrough() {
        Log.i(TAG, "Controller pointer over the $windowName during a game session, pointer input passes through it")
        setWindowTouchable(false)
        passThrough = scope.launch {
            delay(PASS_THROUGH_MS)
            endPassThrough()
        }
    }

    private fun endPassThrough() {
        passThrough = null
        setWindowTouchable(true)
    }

    companion object {
        const val PASS_THROUGH_MS = 3_000L
    }
}

fun isControllerTouchpadEvent(event: MotionEvent): Boolean {
    val pointerEvent = event.isFromSource(InputDevice.SOURCE_MOUSE) ||
        event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE) ||
        event.isFromSource(InputDevice.SOURCE_TOUCHPAD)
    return pointerEvent && event.device?.isControllerTouchpad() == true
}
