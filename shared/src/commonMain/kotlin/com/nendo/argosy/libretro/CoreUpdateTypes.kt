package com.nendo.argosy.libretro

data class PendingCoreUpdate(
    val coreInfo: LibretroCoreRegistry.CoreInfo,
    val installedVersion: String,
    val latestVersion: String
)

data class CoreBulkUpdateResult(
    val updated: List<LibretroCoreRegistry.CoreInfo>,
    val failed: List<LibretroCoreRegistry.CoreInfo>
)
