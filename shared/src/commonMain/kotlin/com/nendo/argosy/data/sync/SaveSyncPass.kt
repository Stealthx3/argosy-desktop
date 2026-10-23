package com.nendo.argosy.data.sync

import com.nendo.argosy.data.preferences.AccountSwitchMarkerStore
import com.nendo.argosy.data.remote.romm.ConnectionState
import com.nendo.argosy.data.remote.romm.RomMRepository
import com.nendo.argosy.domain.usecase.save.CheckNewSavesUseCase
import com.nendo.argosy.util.Logger
import javax.inject.Inject

/**
 * One background save sync pass, shared by Android's WorkManager job and the desktop scheduler.
 * [Outcome.RETRY] asks the caller to run it again later.
 */
class SaveSyncPass @Inject constructor(
    private val checkNewSavesUseCase: CheckNewSavesUseCase,
    private val syncCoordinator: SyncCoordinator,
    private val romMRepository: RomMRepository,
    private val accountSwitchMarkerStore: AccountSwitchMarkerStore
) {

    enum class Outcome { SUCCESS, RETRY }

    suspend fun run(): Outcome {
        if (accountSwitchMarkerStore.isSwitching()) {
            Logger.info(TAG, "[SaveSync] WORKER | Account switch in progress, deferring sync")
            return Outcome.RETRY
        }

        val isConnected = romMRepository.connectionState.value is ConnectionState.Connected
        if (!isConnected) {
            Logger.info(TAG, "[SaveSync] WORKER | RomM not connected, skipping sync")
            return Outcome.SUCCESS
        }

        Logger.info(TAG, "[SaveSync] WORKER | Starting background sync")

        return try {
            val negotiates = (romMRepository.connectionState.value as? ConnectionState.Connected)
                ?.capabilities?.supportsSyncNegotiate == true
            if (negotiates) {
                val summary = syncCoordinator.reconcileAll()
                if (!summary.serverAnswered) {
                    Logger.warn(TAG, "[SaveSync] WORKER | Server did not answer negotiate, retrying later")
                    return Outcome.RETRY
                }
                Logger.info(TAG, "[SaveSync] WORKER | Reconcile complete | games=${summary.planGames}, handled=${summary.planApplied}, conflicts=${summary.planConflicts}")
                return Outcome.SUCCESS
            }
            val checkResult = checkNewSavesUseCase()
            Logger.info(TAG, "[SaveSync] WORKER | Check complete | newSaves=${checkResult.newSavesCount}, platformsChecked=${checkResult.platformsChecked}")

            when (val result = syncCoordinator.processQueue()) {
                is SyncCoordinator.ProcessResult.NotConnected -> {
                    Logger.info(TAG, "[SaveSync] WORKER | Sync skipped - not connected")
                }
                is SyncCoordinator.ProcessResult.Completed -> {
                    Logger.info(TAG, "[SaveSync] WORKER | Sync complete | processed=${result.processed}, failed=${result.failed}")
                }
            }

            Outcome.SUCCESS
        } catch (e: Exception) {
            Logger.error(TAG, "[SaveSync] WORKER | Sync failed, will retry", e)
            Outcome.RETRY
        }
    }

    private companion object {
        const val TAG = "SaveSyncWorker"
    }
}
