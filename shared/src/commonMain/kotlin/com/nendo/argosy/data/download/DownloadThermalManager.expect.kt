package com.nendo.argosy.data.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class DownloadThermalManager {
    companion object {}

    val thermalStatus: StateFlow<ThermalStatus>

    fun start()

    fun stop()
}
