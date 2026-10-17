package com.nendo.argosy.data.installer

import com.nendo.argosy.data.local.entity.ManagedInstallerEntity
import com.nendo.argosy.data.remote.github.GitHubAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class ManagedInstallerManager {
    val job: StateFlow<InstallerJob?>

    fun isInstalled(packageName: String?): Boolean

    fun canInstallPackages(): Boolean

    fun openInstallPermissionSettings()

    fun openImeSettings()

    fun showImePicker()

    fun isImeEnabled(packageName: String): Boolean

    fun isImeActive(packageName: String): Boolean

    fun clearJob()

    fun reconcilePendingInstall()

    suspend fun refresh(rows: List<ManagedInstallerEntity>)

    fun install(row: ManagedInstallerEntity, chosenAsset: GitHubAsset? = null)
}
