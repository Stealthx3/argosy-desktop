package com.nendo.argosy.ui.input

expect class TriggerAxisKeyEmitter {
    companion object {
        fun keyCodeForAxis(axis: Int): Int?
    }

    fun forget(deviceId: Int)
}
