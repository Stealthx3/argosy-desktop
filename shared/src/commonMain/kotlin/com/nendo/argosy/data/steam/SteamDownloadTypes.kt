package com.nendo.argosy.data.steam

import `in`.dragonbra.javasteam.types.KeyValue

sealed class SteamDownloadState {
    data object Idle : SteamDownloadState()
    data class Preparing(val appId: Long, val gameName: String) : SteamDownloadState()
    data class Connecting(val appId: Long, val gameName: String) : SteamDownloadState()
    data class FetchingManifest(val appId: Long, val gameName: String, val depotId: Int) : SteamDownloadState()
    data class Validating(val appId: Long, val gameName: String, val statusDetail: String = "") : SteamDownloadState()
    data class Downloading(
        val appId: Long,
        val gameName: String,
        val progress: Float,
        val currentDepot: Int,
        val totalDepots: Int
    ) : SteamDownloadState()
    data class Moving(val appId: Long, val gameName: String) : SteamDownloadState()
    data class Completed(val appId: Long, val gameName: String, val installPath: String) : SteamDownloadState()
    data class Failed(val appId: Long, val gameName: String, val error: String) : SteamDownloadState()
    data class Paused(val appId: Long, val gameName: String, val progress: Float, val needsVerification: Boolean = false) : SteamDownloadState()
    data class Cleaning(val appId: Long, val gameName: String) : SteamDownloadState()
}

data class DepotInfo(
    val depotId: Int,
    val manifestId: Long,
    val name: String,
    val os: String?,
    val arch: String?,
    val size: Long
)

data class SteamDownloadProgress(
    val appId: Long,
    val gameName: String,
    val coverPath: String?,
    val progress: Float,
    val totalBytes: Long,
    val bytesDownloaded: Long,
    val state: SteamDownloadState,
    val bytesPerSecond: Long = 0L
) {
    val progressPercent: Int get() = (progress * 100).toInt()
}

data class QueuedSteamDownload(
    val appId: Long,
    val gameName: String,
    val coverPath: String?,
    val appInfo: KeyValue?,
    val targetInstallPath: String? = null
)
