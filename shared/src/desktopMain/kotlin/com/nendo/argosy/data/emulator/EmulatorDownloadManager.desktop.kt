package com.nendo.argosy.data.emulator

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class EmulatorDownloadManager @Inject constructor() {
    actual val downloadProgress: StateFlow<EmulatorDownloadProgress?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun downloadAndInstall(emulatorId: String, downloadUrl: String, assetName: String, variant: String?) {
    }

    actual fun cancelDownload() {
    }

    actual fun hasActiveDownload(): Boolean = false

    actual suspend fun clearApkCache(): Boolean = false

    actual fun canInstallPackages(): Boolean = false

    actual fun openInstallPermissionSettings() {
    }
}
