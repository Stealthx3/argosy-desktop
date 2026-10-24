package com.nendo.argosy.data.sync

import com.nendo.argosy.data.emulator.LinuxEmulatorDatabase
import com.nendo.argosy.data.remote.romm.ConnectionState
import com.nendo.argosy.data.remote.romm.RomMRepository
import com.nendo.argosy.data.repository.RetroAchievementsRepository
import com.nendo.argosy.util.Logger
import com.nendo.argosy.util.SafeCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The desktop's stand-in for WorkManager: the jobs Android's application schedules, run as
 * coroutines for as long as Argosy is open. A job that needs RomM waits for a connection, as
 * WorkManager's network constraint waits for one.
 */
@Singleton
class DesktopBackgroundJobs @Inject constructor(
    private val saveSyncPass: SaveSyncPass,
    private val syncCoordinator: SyncCoordinator,
    private val romMRepository: RomMRepository,
    private val saveSyncDownloadObserver: SaveSyncDownloadObserver,
    private val raRepository: dagger.Lazy<RetroAchievementsRepository>,
    private val raPatchBackfill: dagger.Lazy<RAPatchBackfill>
) {
    private val scope = SafeCoroutineScope(Dispatchers.IO, "DesktopBackgroundJobs")
    private var raPatchBackfillJob: Job? = null

    fun start() {
        saveSyncDownloadObserver.start()
        scope.launch { runPeriodicSaveSync() }
        scope.launch { drainQueuePeriodically() }
        scope.launch {
            if (raRepository.get().isLoggedIn()) scheduleRAPatchBackfill()
        }
        scope.launch { LinuxEmulatorDatabase.refresh() }
    }

    @Synchronized
    fun scheduleRAPatchBackfill() {
        if (raPatchBackfillJob?.isActive == true) return
        raPatchBackfillJob = scope.launch {
            var backoff = RA_BACKFILL_BACKOFF
            while (true) {
                awaitRomMConnection()
                if (!raRepository.get().isLoggedIn()) return@launch
                when (raPatchBackfill.get().run()) {
                    RAPatchBackfill.Outcome.COMPLETE -> return@launch
                    RAPatchBackfill.Outcome.RETRY_LATER -> {
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
                    }
                }
            }
        }
    }

    private suspend fun runPeriodicSaveSync() {
        var backoff = SAVE_SYNC_RETRY
        while (true) {
            awaitRomMConnection()
            when (saveSyncPass.run()) {
                SaveSyncPass.Outcome.SUCCESS -> {
                    backoff = SAVE_SYNC_RETRY
                    delay(SAVE_SYNC_INTERVAL)
                }
                SaveSyncPass.Outcome.RETRY -> {
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
                }
            }
        }
    }

    private suspend fun drainQueuePeriodically() {
        while (true) {
            delay(QUEUE_DRAIN_INTERVAL)
            awaitRomMConnection()
            val result = syncCoordinator.processQueue()
            if (result is SyncCoordinator.ProcessResult.Completed && result.processed + result.failed > 0) {
                Logger.info(TAG, "Queue drain | processed=${result.processed}, failed=${result.failed}")
            }
        }
    }

    private suspend fun awaitRomMConnection() {
        romMRepository.connectionState.first { it is ConnectionState.Connected }
    }

    private companion object {
        const val TAG = "DesktopBackgroundJobs"
        val SAVE_SYNC_INTERVAL: Duration = 6.hours
        val SAVE_SYNC_RETRY: Duration = 30.minutes
        val QUEUE_DRAIN_INTERVAL: Duration = 15.minutes
        val RA_BACKFILL_BACKOFF: Duration = 5.minutes
        val MAX_BACKOFF: Duration = 5.hours
    }
}
