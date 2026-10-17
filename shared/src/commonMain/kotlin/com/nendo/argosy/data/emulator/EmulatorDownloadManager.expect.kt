package com.nendo.argosy.data.emulator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class EmulatorDownloadManager {
    val downloadProgress: StateFlow<EmulatorDownloadProgress?>

    fun downloadAndInstall(emulatorId: String, downloadUrl: String, assetName: String, variant: String?)

    fun cancelDownload()

    fun hasActiveDownload(): Boolean

    suspend fun clearApkCache(): Boolean

    fun canInstallPackages(): Boolean

    fun openInstallPermissionSettings()
}
