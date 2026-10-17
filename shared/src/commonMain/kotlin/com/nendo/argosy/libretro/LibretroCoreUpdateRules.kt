package com.nendo.argosy.libretro

import com.nendo.argosy.data.local.entity.CoreVersionEntity

/**
 * The one rule for whether [latestVersion] is an update over [installedVersion]: both are
 * known, they differ, and [latestVersion] is not the build the user rolled back from.
 */
fun LibretroCoreManager.Companion.isUpdateAvailable(
    installedVersion: String?,
    latestVersion: String?,
    blockedVersion: String?
): Boolean =
    installedVersion != null &&
        latestVersion != null &&
        installedVersion != latestVersion &&
        latestVersion != blockedVersion

fun LibretroCoreManager.Companion.pendingUpdateOf(row: CoreVersionEntity): PendingCoreUpdate? {
    val installed = row.installedVersion ?: return null
    val latest = row.latestVersion ?: return null
    if (!row.updateAvailable || !isUpdateAvailable(installed, latest, row.blockedVersion)) {
        return null
    }
    val coreInfo = LibretroCoreRegistry.getCoreById(row.coreId) ?: return null
    return PendingCoreUpdate(coreInfo, installedVersion = installed, latestVersion = latest)
}
