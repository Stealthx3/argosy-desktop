package com.nendo.argosy.data.sync

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.data.download.DownloadManager
import com.nendo.argosy.data.download.MediaDownloadManager
import com.nendo.argosy.data.emulator.EmulatorDownloadManager
import com.nendo.argosy.data.emulator.EmulatorRegistry
import com.nendo.argosy.data.netplay.NetplayJoinService
import com.nendo.argosy.data.netplay.NetplayJoinState
import com.nendo.argosy.data.preferences.AccountSwitchMarkerStore
import com.nendo.argosy.data.preferences.SessionStateStore
import com.nendo.argosy.data.steam.SteamContentManager
import com.nendo.argosy.util.Logger
import com.nendo.argosy.util.PermissionHelper
import dagger.Lazy
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AccountSwitchBlockers"

/**
 * Decides whether it is safe to start tearing down one account's saves.
 *
 * Matches the hard-reset blocker set and adds the switch's own: a library sync mid-drain would
 * upsert rows under whichever identity it started with, a netplay join is a live peer
 * relationship the swap would silently reassign, and an externally launched emulator holds save
 * bytes Argosy is about to archive. An in-game netplay session lives inside the emulator
 * activity and so is covered by the active-session check rather than separately.
 *
 * Pending uploads are deliberately NOT a blocker. Queue rows carry the account that owns them
 * and drain under it later, and blocking on them would make an offline switch impossible --
 * which is the case the whole offline-apply path exists for.
 */
@Singleton
class AccountSwitchBlockerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val markerStore: AccountSwitchMarkerStore,
    private val downloadManager: Lazy<DownloadManager>,
    private val emulatorDownloadManager: Lazy<EmulatorDownloadManager>,
    private val steamContentManager: Lazy<SteamContentManager>,
    private val mediaDownloadManager: Lazy<MediaDownloadManager>,
    private val platformSyncQueue: Lazy<PlatformSyncQueue>,
    private val netplayJoinService: Lazy<NetplayJoinService>,
    private val permissionHelper: PermissionHelper
) {
    private val sessionStateStore by lazy { SessionStateStore(context) }

    suspend fun check(
        confirmedExternalGamesClosed: Boolean
    ): AccountSwitchBlocker? = withContext(Dispatchers.IO) {
        if (markerStore.isSwitching()) return@withContext AccountSwitchBlocker.AlreadySwitching
        if (!confirmedExternalGamesClosed) return@withContext AccountSwitchBlocker.ExitConfirmationRequired
        if (sessionStateStore.hasActiveSession()) return@withContext AccountSwitchBlocker.ActiveSession

        val downloadState = downloadManager.get().state.value
        if (downloadState.activeDownloads.isNotEmpty() || downloadState.queue.isNotEmpty()) {
            return@withContext AccountSwitchBlocker.ActiveDownloads
        }
        if (emulatorDownloadManager.get().hasActiveDownload()) {
            return@withContext AccountSwitchBlocker.EmulatorDownload
        }
        if (steamContentManager.get().hasBlockingDownloadState()) {
            return@withContext AccountSwitchBlocker.SteamDownload
        }
        if (mediaDownloadManager.get().hasBlockingDownloadState()) {
            return@withContext AccountSwitchBlocker.MediaDownload
        }
        if (platformSyncQueue.get().activeJob.value != null ||
            platformSyncQueue.get().libraryQueued.value ||
            platformSyncQueue.get().queuedPlatformIds.value.isNotEmpty()
        ) {
            return@withContext AccountSwitchBlocker.LibrarySyncRunning
        }
        if (netplayJoinService.get().state.value != NetplayJoinState.Idle) {
            return@withContext AccountSwitchBlocker.NetplaySession
        }

        recentlyForegroundEmulator()
    }

    /**
     * An emulator that was in the foreground within the recency window.
     *
     * Instantaneous focus is useless here: opening Argosy to switch accounts is itself what
     * backgrounds the emulator, so the question is "was this recently on screen", which usage
     * stats answers and focus does not. Without the usage-stats permission this returns null,
     * so it is a courtesy check only -- the guarantee is the re-hash immediately before removal.
     */
    private fun recentlyForegroundEmulator(): AccountSwitchBlocker? {
        val packages = EmulatorRegistry.getAll().map { it.packageName }.toSet()
        val stamps = permissionHelper.lastForegroundTimestamps(context, packages)
        if (stamps.isEmpty()) {
            Logger.debug(TAG, "No usage-stats answer for emulator foreground check; relying on pre-removal re-hash")
            return null
        }
        val now = System.currentTimeMillis()
        val recent = stamps
            .filterValues { now - it <= RECENT_FOREGROUND_WINDOW_MS }
            .maxByOrNull { it.value } ?: return null
        return AccountSwitchBlocker.ExternalGameRecentlyForeground(
            packageName = recent.key,
            secondsAgo = (now - recent.value) / 1000
        )
    }

    companion object {
        private const val RECENT_FOREGROUND_WINDOW_MS = 2 * 60 * 1000L
    }
}
