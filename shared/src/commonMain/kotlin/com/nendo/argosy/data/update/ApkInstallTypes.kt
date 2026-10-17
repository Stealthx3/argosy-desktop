package com.nendo.argosy.data.update

data class PendingApkInstall(
    val gameId: Long,
    val apkPath: String,
    val expectedPackageName: String?
)

sealed class ApkInstallState {
    data object Idle : ApkInstallState()
    data class WaitingForInstall(val gameId: Long, val apkPath: String) : ApkInstallState()
    data class InstallComplete(val gameId: Long, val packageName: String) : ApkInstallState()
    data class InstallFailed(val gameId: Long, val reason: String) : ApkInstallState()
}
