package com.nendo.argosy.data.steam

data class SteamInstallVolume(
    val path: String,
    val label: String,
    val freeBytes: Long,
    val hasGnPath: Boolean,
    val target: SteamInstallTarget
)
