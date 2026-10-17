package com.nendo.argosy.ui.input

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class TriggerAxisKeyEmitter @Inject constructor() {
    actual companion object {
        actual fun keyCodeForAxis(axis: Int): Int? = null
    }

    actual fun forget(deviceId: Int) {
    }
}
