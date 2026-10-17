package com.nendo.argosy.data.installer

import com.nendo.argosy.data.local.entity.ManagedInstallerEntity
import com.nendo.argosy.data.remote.github.GitHubAsset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class ManagedInstallerManager @Inject constructor() {
    actual val job: StateFlow<InstallerJob?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun isInstalled(packageName: String?): Boolean = false

    actual fun canInstallPackages(): Boolean = false

    actual fun openImeSettings() {
    }

    actual fun showImePicker() {
    }

    actual fun isImeEnabled(packageName: String): Boolean = false

    actual fun isImeActive(packageName: String): Boolean = false

    actual fun clearJob() {
    }

    actual fun reconcilePendingInstall() {
    }

    actual suspend fun refresh(rows: List<ManagedInstallerEntity>) {
    }

    actual fun install(row: ManagedInstallerEntity, chosenAsset: GitHubAsset?) {
    }

    actual fun openInstallPermissionSettings() {
    }
}
