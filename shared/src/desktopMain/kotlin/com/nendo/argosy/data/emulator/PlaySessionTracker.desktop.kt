package com.nendo.argosy.data.emulator

import com.nendo.argosy.core.event.GameUpdateBus
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.PlaySessionDao
import com.nendo.argosy.data.local.entity.PlaySessionEntity
import com.nendo.argosy.data.preferences.SessionStateStore
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.data.remote.romm.RomMRepository
import com.nendo.argosy.data.repository.GameUserOverlayWriter
import com.nendo.argosy.data.repository.SaveCacheManager
import com.nendo.argosy.data.sync.SaveRecoveryGate
import com.nendo.argosy.domain.usecase.achievement.ReconcileAchievementsOnSessionEndUseCase
import com.nendo.argosy.platform.HostGameProcess
import com.nendo.argosy.platform.PlatformApplication
import com.nendo.argosy.platform.PlatformBuild
import com.nendo.argosy.platform.PlatformSettings
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.util.Logger
import com.nendo.argosy.util.SafeCoroutineScope
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * its play session row, play time and the RomM "incomplete" status on end, as on Android. Save
 * sync on session end is not wired on the desktop yet: every end reports its save as exempt.
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
    private val sessionRecapFactsRecorder: SessionRecapFactsRecorder
) {
    actual companion object {
        private const val TAG = "PlaySessionTracker"
        private const val MIN_PLAY_SECONDS_FOR_COMPLETION = 20
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
        if (_activeSession.value != null) return
        val orphaned = preferencesRepository.getPersistedSession() ?: return
        if (HostGameProcess.running.value != null) return
        Logger.warn(TAG, "[SaveSync] SESSION gameId=${orphaned.gameId} | Session from ${orphaned.startTime} outlived its process; its end time is unknown, so no play time is recorded")
        releaseSession()
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
                checkOrphanedSession()
                return SessionEndResult.Skipped
            }
            _activeSession.value = null
            val endTime = Instant.now()
            val sessionDuration = Duration.between(session.startTime, endTime)
            Logger.debug(TAG, "[SaveSync] SESSION gameId=${session.gameId} | Session ended | duration=${sessionDuration.seconds}s, emulator=${session.emulatorPackage}")
            try {
                recordPlaySession(session, endTime)
                reconcileAchievementsInBackground(session.gameId)
                recordPlayTime(session, sessionDuration)
                markGameIncompleteIfNeeded(session, sessionDuration)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, "[SaveSync] SESSION gameId=${session.gameId} | Recording the session failed", e)
            }
            releaseSession()
            _externalSessionClosed.tryEmit(Unit)
            return SessionEndResult.Skipped
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

    private suspend fun releaseSession() {
        preferencesRepository.clearActiveSession()
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
                releaseSession()
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
