package com.nendo.argosy.ui.input

import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent
import com.nendo.argosy.platform.name
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
import com.nendo.argosy.platform.PlatformMotionEvent as MotionEvent
import com.nendo.argosy.platform.actionMasked
import com.nendo.argosy.platform.getAxisValue
import com.nendo.argosy.core.input.AndroidMotionAxes

expect class TriggerAxisKeyEmitter {
    companion object {
        fun keyCodeForAxis(axis: Int): Int?
    }

    fun emit(event: MotionEvent): List<KeyEvent>

    fun emitByAxis(event: MotionEvent): List<Pair<Int, KeyEvent>>

    fun forget(deviceId: Int)
}
