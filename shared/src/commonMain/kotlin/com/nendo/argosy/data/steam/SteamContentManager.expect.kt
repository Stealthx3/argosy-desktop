package com.nendo.argosy.data.steam

import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.types.KeyValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class SteamContentManager {
    class IncompleteDownload {
        val appId: Long
        val installPath: String
        val stagingPath: String
        val bytesDownloaded: Long
        val gameName: String?
    }

    val downloadState: StateFlow<SteamDownloadState>

    val activeDownload: StateFlow<SteamDownloadProgress?>

    val downloadQueue: StateFlow<List<QueuedSteamDownload>>

    val completedDownloads: StateFlow<List<SteamDownloadProgress>>

    fun clearCompletedDownloads()

    fun initialize(client: SteamClient, apps: SteamApps, cm: CallbackManager)

    suspend fun hasPendingDownloads(): Boolean

    suspend fun restorePausedDownloads()

    fun onDownloadSlotFreed()

    fun hasActiveSteamDownload(): Boolean

    fun hasBlockingDownloadState(): Boolean

    suspend fun clearDownloadData(): Boolean

    fun isConnected(): Boolean

    fun onDisconnected()

    suspend fun ensureConnected(): Boolean

    fun notifyConnected()

    suspend fun fetchAppInfo(appId: Int): KeyValue

    fun queueDownloadOptimistic(appId: Long, gameName: String, coverPath: String?)

    fun queueDownload(appId: Long, gameName: String, appInfo: KeyValue, coverPath: String?)

    suspend fun downloadGame(appId: Long, gameName: String, appInfo: KeyValue, coverPath: String?)

    suspend fun checkForUpdate(appId: Long, currentBuildId: Long?, appInfo: KeyValue): Boolean

    fun getEstimatedSize(appInfo: KeyValue): Long

    suspend fun isGameInstalled(appId: Long): Boolean

    fun cancelDownload()

    fun cancelQueuedDownload(appId: Long)

    fun pauseDownload()

    suspend fun deleteGame(appId: Long): Boolean

    suspend fun discoverLocalSteamGames(): Int

    suspend fun getIncompleteDownloads(): List<IncompleteDownload>

    suspend fun recoverDownload(appId: Long): Boolean

    suspend fun cleanupIncompleteDownload(appId: Long): Boolean

    suspend fun recoverAllIncomplete(): Int

    fun cleanup()
}
