package com.nendo.argosy.data.update

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class ApkInstallManager @Inject constructor() {
    actual val state: StateFlow<ApkInstallState> = TODO("desktop: state")

    actual fun canInstallPackages(): Boolean = false

    actual fun openInstallPermissionSettings() {
    }

    actual fun resetState() {
    }

    actual suspend fun installApkForGame(gameId: Long): Boolean = false
}
