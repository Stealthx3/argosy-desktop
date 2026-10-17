package com.nendo.argosy.data.download

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class DownloadThermalManager @Inject constructor() {
    actual companion object {}

    actual val thermalStatus: StateFlow<ThermalStatus> = kotlinx.coroutines.flow.MutableStateFlow(ThermalStatus())

    actual fun start() {
    }

    actual fun stop() {
    }
}
