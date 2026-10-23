package com.nendo.argosy.data.emulator

import com.nendo.argosy.core.event.GameUpdateBus
import com.nendo.argosy.core.notification.NotificationDuration
import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.NotificationType
import com.nendo.argosy.data.local.dao.GameArtDao
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.PlaySessionDao
import com.nendo.argosy.data.local.dao.resolved
import com.nendo.argosy.data.local.entity.PlaySessionEntity
import com.nendo.argosy.data.preferences.PersistedSession
import com.nendo.argosy.data.preferences.SessionStateStore
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.data.remote.romm.RomMRepository
import com.nendo.argosy.data.repository.GameUserOverlayWriter
import com.nendo.argosy.data.repository.SaveCacheManager
import com.nendo.argosy.data.sync.SaveRecoveryGate
import com.nendo.argosy.domain.usecase.achievement.ReconcileAchievementsOnSessionEndUseCase
import com.nendo.argosy.domain.usecase.save.SyncSaveOnSessionEndUseCase
import com.nendo.argosy.domain.usecase.state.StateSyncResult
import com.nendo.argosy.domain.usecase.state.SyncStatesOnSessionEndUseCase
import com.nendo.argosy.platform.HostGameProcess
import com.nendo.argosy.platform.PlatformApplication
import com.nendo.argosy.platform.PlatformBuild
import com.nendo.argosy.platform.PlatformSettings
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getString
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.util.Logger
import com.nendo.argosy.util.SafeCoroutineScope
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Play sessions for games run by a host emulator process ([HostGameProcess]). A session records
 * its play session row, play time and the RomM "incomplete" status on end, then caches and syncs
 * its states and save through [SessionSaveFinalizer], as on Android.
 */
@Singleton
actual class PlaySessionTracker @Inject constructor(
    private val application: PlatformApplication,
    private val gameDao: GameDao,
    private val overlayWriter: GameUserOverlayWriter,
    private val playSessionDao: PlaySessionDao,
    private val romMRepository: dagger.Lazy<RomMRepository>,
    private val preferencesRepository: UserPreferencesRepository,
    private val gameUpdateBus: GameUpdateBus,
    private val saveRecoveryGate: SaveRecoveryGate,
    private val reconcileAchievementsOnSessionEndUseCase: dagger.Lazy<ReconcileAchievementsOnSessionEndUseCase>,
    private val sessionRecapFactsRecorder: SessionRecapFactsRecorder,
    private val sessionSaveFinalizer: SessionSaveFinalizer,
    private val syncStatesOnSessionEndUseCase: dagger.Lazy<SyncStatesOnSessionEndUseCase>,
    private val notificationManager: NotificationManager,
    private val gameArtDao: GameArtDao
) {
    actual companion object {
        private const val TAG = "PlaySessionTracker"
        private const val MIN_PLAY_SECONDS_FOR_COMPLETION = 20
        private const val MAX_SESSION_RECOVERY_ATTEMPTS = 3
    }

    private val scope = SafeCoroutineScope(Dispatchers.IO, "PlaySessionTracker")
    private val sessionStateStore by lazy { SessionStateStore(application) }
    private val saveObserved = AtomicBoolean(false)

    private val _activeSession = MutableStateFlow<ActiveSession?>(null)
    actual val activeSession: StateFlow<ActiveSession?> = _activeSession.asStateFlow()
    actual val hasActiveSession: StateFlow<Boolean> = _activeSession
        .map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _pendingSessionConflict = MutableStateFlow<SaveConflictEvent?>(null)
    actual val pendingSessionConflict: StateFlow<SaveConflictEvent?> = _pendingSessionConflict.asStateFlow()

    actual fun clearPendingSessionConflict() {
        _pendingSessionConflict.value = null
    }

    private val _externalSessionClosed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    actual val externalSessionClosed: SharedFlow<Unit> = _externalSessionClosed.asSharedFlow()

    private val _saveWrites = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    actual val saveWrites: SharedFlow<Unit> = _saveWrites

    actual suspend fun checkOrphanedSession() {
        if (!saveRecoveryGate.tryClaimSessionEnd()) {
            Logger.debug(TAG, "[SaveSync] ORPHAN | Skipping orphan check, endSession is handling recovery")
            saveRecoveryGate.markComplete()
            return
        }
        try {
            recoverOrphanedSession()
        } finally {
            saveRecoveryGate.releaseSessionEnd()
            saveRecoveryGate.markComplete()
        }
    }

    private suspend fun recoverOrphanedSession(): Boolean {
        if (_activeSession.value != null) return false
        val orphaned = preferencesRepository.getPersistedSession() ?: return false
        if (HostGameProcess.running.value != null) return false
        Logger.warn(TAG, "[SaveSync] ORPHAN gameId=${orphaned.gameId} | Session from ${orphaned.startTime} outlived its process; its end time is unknown, so no play time is recorded")
        if (orphaned.variantFileId == null && !orphaned.isNetplayGuest) {
            syncStateDataLogged(orphaned.gameId, orphaned.emulatorPackage, "ORPHAN")
        }
        val saved = recoverOrphanedSave(orphaned)
        releaseSession(keepRecord = !saved.isSettled)
        return true
    }

    private suspend fun recoverOrphanedSave(orphaned: PersistedSession): SessionSaveOutcome {
        val outcome = try {
            finalizeSave(orphaned.toSaveInput())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.error(TAG, "[SaveSync] ORPHAN gameId=${orphaned.gameId} | Recovery failed, keeping the session for the next attempt", e)
            return SessionSaveOutcome.CacheFailed
        }
        Logger.info(TAG, "[SaveSync] ORPHAN gameId=${orphaned.gameId} | Recovery outcome=${outcome::class.simpleName} settled=${outcome.isSettled}")
        val sync = (outcome as? SessionSaveOutcome.Synced)?.sync
        if (sync is SyncSaveOnSessionEndUseCase.Result.Uploaded) {
            val game = gameDao.getById(orphaned.gameId)
            notificationManager.show(
                title = NotificationText.Res(R.string.sync_session_save_uploaded_orphan),
                subtitle = game?.title?.let { NotificationText.Raw(it) },
                type = NotificationType.SUCCESS,
                imagePath = coverPathFor(orphaned.gameId),
                duration = NotificationDuration.MEDIUM,
                key = "sync-${orphaned.gameId}",
                immediate = true
            )
        }
        return outcome
    }

    actual fun startSession(
        gameId: Long,
        emulatorPackage: String,
        coreName: String?,
        isHardcore: Boolean,
        isNewGame: Boolean,
        isNetplayGuest: Boolean,
        variantFileId: Long?,
        origin: LaunchOrigin
    ) {
        val running = _activeSession.value
        if (running != null) {
            Logger.error(TAG, "[SaveSync] SESSION gameId=$gameId | startSession refused, session for gameId=${running.gameId} (emulator=${running.emulatorPackage}) is still active")
            return
        }
        saveObserved.set(false)
        val startTime = Instant.now()
        sessionRecapFactsRecorder.onSessionStarted(scope, gameId, emulatorPackage, startTime)
        _activeSession.value = ActiveSession(
            gameId = gameId,
            startTime = startTime,
            emulatorPackage = emulatorPackage,
            coreName = coreName,
            isHardcore = isHardcore,
            isNewGame = isNewGame,
            isNetplayGuest = isNetplayGuest,
            variantFileId = variantFileId,
            origin = origin
        )
        Logger.debug(TAG, "[SaveSync] SESSION gameId=$gameId | Session started | emulator=$emulatorPackage, core=$coreName, origin=$origin")
        scope.launch {
            preferencesRepository.persistActiveSession(
                gameId = gameId,
                emulatorPackage = emulatorPackage,
                startTime = startTime,
                coreName = coreName,
                isHardcore = isHardcore,
                channelName = null,
                variantFileId = variantFileId,
                origin = origin,
                isNetplayGuest = isNetplayGuest
            )
            sessionStateStore.setActiveSession(gameId, null, isHardcore, startTime.toEpochMilli(), emulatorPackage)
        }
    }

    actual fun updateSessionHardcore(gameId: Long, isHardcore: Boolean) {
        _activeSession.value?.takeIf { it.gameId == gameId }?.let { _activeSession.value = it.copy(isHardcore = isHardcore) }
    }

    actual suspend fun endSession(stopService: Boolean, skipSaveSync: Boolean): SessionEndResult {
        if (!saveRecoveryGate.tryClaimSessionEnd()) {
            Logger.debug(TAG, "[SaveSync] SESSION | endSession already in progress, skipping")
            return SessionEndResult.Skipped
        }
        try {
            val session = _activeSession.value ?: run {
                if (!recoverOrphanedSession()) releaseSession(keepRecord = false)
                return SessionEndResult.Skipped
            }
            _activeSession.value = null
            val endTime = Instant.now()
            val sessionDuration = Duration.between(session.startTime, endTime)
            Logger.debug(TAG, "[SaveSync] SESSION gameId=${session.gameId} | Session ended | duration=${sessionDuration.seconds}s, emulator=${session.emulatorPackage}")
            try {
                recordPlaySession(session, endTime)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, "[SaveSync] SESSION gameId=${session.gameId} | Recording the play session failed", e)
            }
            reconcileAchievementsInBackground(session.gameId)

            val effectiveSkipSaveSync = skipSaveSync || session.isNetplayGuest || session.variantFileId != null
            val outcome = try {
                val saveOutcome = coroutineScope {
                    async {
                        recordPlayTime(session, sessionDuration)
                        markGameIncompleteIfNeeded(session, sessionDuration)
                        if (effectiveSkipSaveSync) {
                            SessionSaveOutcome.Exempt
                        } else {
                            syncStateDataLogged(session.gameId, session.emulatorPackage, "SESSION")
                            finalizeSave(session.toSaveInput())
                        }
                    }.await()
                }

                if (saveOutcome is SessionSaveOutcome.Synced) {
                    handleSaveSyncResult(session, saveOutcome.sync)
                }
                releaseSession(keepRecord = !saveOutcome.isSettled)

                when (saveOutcome) {
                    is SessionSaveOutcome.Synced -> when (saveOutcome.cache) {
                        is SaveCacheManager.CacheResult.Duplicate -> SessionEndResult.Duplicate
                        else -> SessionEndResult.Success
                    }
                    SessionSaveOutcome.CacheFailed -> SessionEndResult.Error("Failed to cache save")
                    is SessionSaveOutcome.Unreadable -> SessionEndResult.SaveUnreadable(saveOutcome.dirPath, saveOutcome.emulatorId)
                    SessionSaveOutcome.Exempt, SessionSaveOutcome.NothingToSave -> SessionEndResult.Skipped
                }
            } catch (e: CancellationException) {
                releaseSession(keepRecord = true)
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, "[SaveSync] SESSION gameId=${session.gameId} | Session end failed, keeping the session for recovery", e)
                releaseSession(keepRecord = true)
                SessionEndResult.Error(e.message ?: "Unknown error")
            }
            _externalSessionClosed.tryEmit(Unit)
            return outcome
        } finally {
            saveRecoveryGate.releaseSessionEnd()
        }
    }

    private suspend fun recordPlaySession(session: ActiveSession, endTime: Instant) {
        if (playSessionDao.existsByGameAndStart(session.gameId, session.startTime)) return
        val game = gameDao.getById(session.gameId)
        val prefs = preferencesRepository.userPreferences.first()
        val recapFacts = sessionRecapFactsRecorder.factsFor(game, session.emulatorPackage, session.startTime, endTime)
        val activePlayMs = Duration.between(session.startTime, endTime).toMillis()
        val sessionId = playSessionDao.insert(
            PlaySessionEntity(
                userId = prefs.socialUserId,
                ownerUserId = prefs.rommUserId,
                gameId = session.gameId,
                igdbId = game?.igdbId,
                gameTitle = game?.title ?: "Unknown",
                platformSlug = game?.platformSlug ?: "unknown",
                startTime = session.startTime,
                endTime = endTime,
                continued = false,
                deviceId = PlatformSettings.Secure.getString(application.contentResolver, PlatformSettings.Secure.ANDROID_ID) ?: "",
                deviceManufacturer = PlatformBuild.MANUFACTURER,
                deviceModel = PlatformBuild.MODEL,
                activePlayMs = activePlayMs,
                standbyMs = 0,
                raGameId = recapFacts.raGameId,
                richPresence = recapFacts.richPresence,
                richPresenceAt = recapFacts.richPresenceAt
            )
        )
        sessionRecapFactsRecorder.onSessionRecorded(
            scope, sessionId, session.gameId, session.emulatorPackage, recapFacts, session.startTime, endTime
        )
    }

    private suspend fun recordPlayTime(session: ActiveSession, playDuration: Duration) {
        val seconds = playDuration.toMillis() / 1000
        val minutes = ((seconds + 30) / 60).toInt()
        if (minutes <= 0) {
            Logger.debug(TAG, "Session too short to record: ${seconds}s")
            return
        }
        overlayWriter.addPlayTime(session.gameId, minutes)
        gameDao.getById(session.gameId)?.let { updatedGame ->
            gameUpdateBus.emit(GameUpdateBus.GameUpdate(gameId = session.gameId, playTimeMinutes = updatedGame.playTimeMinutes))
        }
    }

    private suspend fun markGameIncompleteIfNeeded(session: ActiveSession, sessionDuration: Duration) {
        if (sessionDuration.seconds < MIN_PLAY_SECONDS_FOR_COMPLETION) return
        val game = gameDao.getById(session.gameId) ?: return
        if (game.rommId == null || game.status != null) return
        romMRepository.get().updateUserStatus(session.gameId, "incomplete")
        gameUpdateBus.emit(GameUpdateBus.GameUpdate(gameId = session.gameId, status = "incomplete"))
    }

    private fun reconcileAchievementsInBackground(gameId: Long) {
        scope.launch {
            try {
                reconcileAchievementsOnSessionEndUseCase.get()(gameId)
            } catch (e: Exception) {
                Logger.warn(TAG, "[Achievements] Session-end reconcile failed for gameId=$gameId", e)
            }
        }
    }

    private suspend fun finalizeSave(input: SessionSaveInput): SessionSaveOutcome {
        val outcome = sessionSaveFinalizer.finalize(input)
        if (outcome is SessionSaveOutcome.Synced) {
            surfaceConflict(outcome)
        }
        return outcome
    }

    private fun surfaceConflict(outcome: SessionSaveOutcome.Synced) {
        val conflict = outcome.sync as? SyncSaveOnSessionEndUseCase.Result.Conflict ?: return
        Logger.debug(TAG, "[SaveSync] SESSION gameId=${conflict.gameId} | Sync result: CONFLICT | conflictId=${outcome.conflictId}, local=${conflict.upload.localTimestamp}, server=${conflict.upload.serverTimestamp}")
        _pendingSessionConflict.value = SaveConflictEvent(
            gameId = conflict.gameId,
            emulatorId = conflict.emulatorId,
            channelName = conflict.channelName,
            localTimestamp = conflict.upload.localTimestamp,
            serverTimestamp = conflict.upload.serverTimestamp,
            serverDeviceName = conflict.upload.serverDeviceName,
            conflictId = outcome.conflictId,
            isHardcoreDowngrade = conflict.upload.isHardcoreDowngrade
        )
    }

    private suspend fun handleSaveSyncResult(session: ActiveSession, result: SyncSaveOnSessionEndUseCase.Result) {
        val game = gameDao.getById(session.gameId)
        when (result) {
            is SyncSaveOnSessionEndUseCase.Result.Uploaded -> notificationManager.show(
                title = NotificationText.Res(R.string.sync_session_save_uploaded),
                subtitle = game?.title?.let { NotificationText.Raw(it) },
                type = NotificationType.SUCCESS,
                imagePath = coverPathFor(session.gameId),
                duration = NotificationDuration.MEDIUM,
                key = "sync-${session.gameId}",
                immediate = true
            )
            is SyncSaveOnSessionEndUseCase.Result.NoChange -> notificationManager.show(
                title = NotificationText.Res(R.string.sync_session_save_unchanged),
                subtitle = game?.title?.let { NotificationText.Raw(it) },
                type = NotificationType.INFO,
                imagePath = coverPathFor(session.gameId),
                duration = NotificationDuration.SHORT,
                key = "sync-${session.gameId}",
                immediate = true
            )
            is SyncSaveOnSessionEndUseCase.Result.Error -> {
                Logger.error(TAG, "[SaveSync] SESSION gameId=${session.gameId} | Sync result: ERROR | ${result.message}")
                notificationManager.show(
                    title = NotificationText.Res(R.string.sync_session_save_upload_failed),
                    subtitle = NotificationText.Res(
                        R.string.sync_session_save_upload_failed_subtitle,
                        listOf(
                            game?.title
                                ?: application.getString(R.string.sync_session_save_upload_failed_name_fallback),
                            result.message
                        )
                    ),
                    type = NotificationType.ERROR,
                    imagePath = coverPathFor(session.gameId),
                    duration = NotificationDuration.MEDIUM,
                    key = "sync-${session.gameId}",
                    immediate = true
                )
            }
            else -> Logger.debug(TAG, "[SaveSync] SESSION gameId=${session.gameId} | Sync result: ${result::class.simpleName}")
        }
    }

    private suspend fun coverPathFor(gameId: Long): String? = gameArtDao.resolved(gameId).coverPath

    private suspend fun syncStateDataLogged(gameId: Long, emulatorPackage: String, phase: String) {
        try {
            when (val result = syncStatesOnSessionEndUseCase.get()(gameId, emulatorPackage)) {
                is StateSyncResult.Cached -> Logger.debug(TAG, "Cached ${result.count} states for game $gameId")
                is StateSyncResult.Error -> Logger.error(TAG, "State sync error: ${result.reason}")
                else -> Logger.debug(TAG, "[StateSync] $phase gameId=$gameId | ${result::class.simpleName}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.error(TAG, "[StateSync] $phase gameId=$gameId | State sync failed", e)
        }
    }

    private suspend fun releaseSession(keepRecord: Boolean) {
        if (!keepRecord) {
            preferencesRepository.clearActiveSession()
        } else if (!preferencesRepository.keepSessionForRetry(MAX_SESSION_RECOVERY_ATTEMPTS)) {
            Logger.error(TAG, "[SaveSync] SESSION | Save capture failed $MAX_SESSION_RECOVERY_ATTEMPTS times; dropped the session record")
        }
        sessionStateStore.clearSession()
    }

    actual fun sawSaveActivity(): Boolean = saveObserved.get()

    actual fun markSaveObserved() {
        saveObserved.set(true)
    }

    actual fun reportSaveWritten() {
        markSaveObserved()
        _saveWrites.tryEmit(Unit)
    }

    actual fun getSessionDuration(): Duration? =
        _activeSession.value?.let { Duration.between(it.startTime, Instant.now()) }

    actual fun cancelSession() {
        val session = _activeSession.value ?: return
        if (!saveRecoveryGate.tryClaimSessionEnd()) return
        _activeSession.value = null
        Logger.debug(TAG, "[SaveSync] SESSION gameId=${session.gameId} | Cancelled (no save backup)")
        scope.launch {
            try {
                releaseSession(keepRecord = false)
                _externalSessionClosed.tryEmit(Unit)
            } finally {
                saveRecoveryGate.releaseSessionEnd()
            }
        }
    }

    actual fun endSessionInBackground(skipSaveSync: Boolean, stopService: Boolean): Job =
        scope.launch { endSession(stopService = stopService, skipSaveSync = skipSaveSync) }

    actual suspend fun cacheCurrentSessionForQuit(): SaveCacheManager.CacheResult? = null

    actual suspend fun cacheCurrentSessionStatesForQuit() {
    }

    actual fun forceStopService() {
    }

    actual fun canResumeSession(gameId: Long, variantFileId: Long?): Boolean {
        val session = _activeSession.value ?: return false
        if (session.gameId != gameId || session.variantFileId != variantFileId) return false
        return HostGameProcess.running.value?.packageName == session.emulatorPackage
    }
}

private fun ActiveSession.toSaveInput() = SessionSaveInput(
    gameId = gameId,
    emulatorPackage = emulatorPackage,
    coreName = coreName,
    isHardcore = isHardcore,
    channelName = channelName,
    startTime = startTime,
    variantFileId = variantFileId,
    isNetplayGuest = isNetplayGuest
)

private fun PersistedSession.toSaveInput() = SessionSaveInput(
    gameId = gameId,
    emulatorPackage = emulatorPackage,
    coreName = coreName,
    isHardcore = isHardcore,
    channelName = channelName,
    startTime = startTime,
    variantFileId = variantFileId,
    isNetplayGuest = isNetplayGuest
)
