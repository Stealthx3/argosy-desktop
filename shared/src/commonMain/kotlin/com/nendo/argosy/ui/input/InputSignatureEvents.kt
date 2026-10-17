package com.nendo.argosy.ui.input

import android.view.KeyEvent
import android.view.MotionEvent

fun InputSignature.Companion.of(event: KeyEvent): InputSignature = InputSignature(
    eventTime = event.eventTime,
    deviceId = event.deviceId,
    keyCode = event.keyCode,
    action = event.action
)

fun InputSignature.Companion.of(event: MotionEvent): InputSignature = InputSignature(
    eventTime = event.eventTime,
    deviceId = event.deviceId,
    keyCode = 0,
    action = event.actionMasked
)
