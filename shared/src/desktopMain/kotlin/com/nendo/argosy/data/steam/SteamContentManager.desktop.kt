package com.nendo.argosy.data.steam

import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.types.KeyValue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class SteamContentManager @Inject constructor() {
    actual data class IncompleteDownload(
        actual val appId: Long,
        actual val installPath: String,
        actual val stagingPath: String,
        actual val bytesDownloaded: Long,
        actual val gameName: String?
    )

    actual val downloadState: StateFlow<SteamDownloadState> = TODO("desktop: downloadState")

    actual val activeDownload: StateFlow<SteamDownloadProgress?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val downloadQueue: StateFlow<List<QueuedSteamDownload>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val completedDownloads: StateFlow<List<SteamDownloadProgress>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual fun clearCompletedDownloads() {
    }

    actual fun initialize(client: SteamClient, apps: SteamApps, cm: CallbackManager) {
    }

    actual suspend fun hasPendingDownloads(): Boolean = false

    actual suspend fun restorePausedDownloads() {
    }

    actual fun onDownloadSlotFreed() {
    }

    actual fun hasActiveSteamDownload(): Boolean = false

    actual fun hasBlockingDownloadState(): Boolean = false

    actual suspend fun clearDownloadData(): Boolean = false

    actual fun isConnected(): Boolean = false

    actual fun onDisconnected() {
    }

    actual suspend fun ensureConnected(): Boolean = false

    actual fun notifyConnected() {
    }

    actual suspend fun fetchAppInfo(appId: Int): KeyValue = TODO("desktop: fetchAppInfo")

    actual fun queueDownloadOptimistic(appId: Long, gameName: String, coverPath: String?) {
    }

    actual fun queueDownload(appId: Long, gameName: String, appInfo: KeyValue, coverPath: String?) {
    }

    actual suspend fun downloadGame(appId: Long, gameName: String, appInfo: KeyValue, coverPath: String?) {
    }

    actual suspend fun checkForUpdate(appId: Long, currentBuildId: Long?, appInfo: KeyValue): Boolean = false

    actual fun getEstimatedSize(appInfo: KeyValue): Long = 0L

    actual suspend fun isGameInstalled(appId: Long): Boolean = false

    actual fun cancelDownload() {
    }

    actual fun cancelQueuedDownload(appId: Long) {
    }

    actual fun pauseDownload() {
    }

    actual suspend fun deleteGame(appId: Long): Boolean = false

    actual suspend fun discoverLocalSteamGames(): Int = 0

    actual suspend fun getIncompleteDownloads(): List<IncompleteDownload> = emptyList()

    actual suspend fun recoverDownload(appId: Long): Boolean = false

    actual suspend fun cleanupIncompleteDownload(appId: Long): Boolean = false

    actual suspend fun recoverAllIncomplete(): Int = 0

    actual fun cleanup() {
    }
}
