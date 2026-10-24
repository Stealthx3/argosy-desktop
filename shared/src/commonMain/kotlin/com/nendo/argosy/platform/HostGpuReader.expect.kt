package com.nendo.argosy.platform

import com.nendo.argosy.data.model.HostGpuInfo

/**
 * Describes the graphics hardware Argosy runs on. [isSupported] is false where the platform has
 * its own GPU driver screen instead.
 */
expect class HostGpuReader {
    val isSupported: Boolean

    suspend fun read(): HostGpuInfo
}
