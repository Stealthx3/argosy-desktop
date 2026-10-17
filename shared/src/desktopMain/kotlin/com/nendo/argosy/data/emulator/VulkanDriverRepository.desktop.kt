package com.nendo.argosy.data.emulator

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class VulkanDriverRepository @Inject constructor() {
    actual companion object {}

    actual suspend fun customDriversSupported(): Boolean = false

    actual fun vulkanSupported(): Boolean = false

    actual suspend fun listInstalled(): List<InstalledVulkanDriver> = emptyList()

    actual suspend fun listInstallablePackages(): List<File> = emptyList()

    actual suspend fun installFromDownloads(fileName: String): Result<InstalledVulkanDriver> = kotlin.Result.failure(UnsupportedOperationException("not available on desktop"))
}
