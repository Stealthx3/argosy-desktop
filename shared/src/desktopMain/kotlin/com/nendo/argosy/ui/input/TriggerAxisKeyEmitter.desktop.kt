package com.nendo.argosy.ui.input

import javax.inject.Inject
import javax.inject.Singleton
import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent
import com.nendo.argosy.core.input.AndroidKeyCodes
import com.nendo.argosy.platform.PlatformMotionEvent as MotionEvent
import com.nendo.argosy.core.input.AndroidMotionAxes

@Singleton
actual class TriggerAxisKeyEmitter @Inject constructor() {
    actual companion object {
        actual fun keyCodeForAxis(axis: Int): Int? = null
    }

    actual fun forget(deviceId: Int) {
    }

    actual fun emit(event: MotionEvent): List<KeyEvent> = emptyList()

    actual fun emitByAxis(event: MotionEvent): List<Pair<Int, KeyEvent>> = emptyList()
}
