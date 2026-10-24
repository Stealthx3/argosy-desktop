package com.nendo.argosy.platform

import com.nendo.argosy.data.model.HostGpuInfo
import javax.inject.Inject

actual class HostGpuReader @Inject constructor() {
    actual val isSupported: Boolean = false

    actual suspend fun read(): HostGpuInfo = HostGpuInfo()
}
