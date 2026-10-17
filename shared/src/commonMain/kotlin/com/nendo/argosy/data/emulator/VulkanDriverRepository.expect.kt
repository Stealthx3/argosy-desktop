package com.nendo.argosy.data.emulator

import java.io.File

expect class VulkanDriverRepository {
    companion object {}

    suspend fun customDriversSupported(): Boolean

    fun vulkanSupported(): Boolean

    suspend fun listInstalled(): List<InstalledVulkanDriver>

    suspend fun listInstallablePackages(): List<File>

    suspend fun installFromDownloads(fileName: String): Result<InstalledVulkanDriver>
}
