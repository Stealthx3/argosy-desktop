package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.repository.SaveCacheManager
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class PlaySessionTracker @Inject constructor() {
    actual companion object {}

    actual val activeSession: StateFlow<ActiveSession?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val hasActiveSession: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val pendingSessionConflict: StateFlow<SaveConflictEvent?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun clearPendingSessionConflict() {
    }

    actual val externalSessionClosed: SharedFlow<Unit> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual suspend fun checkOrphanedSession() {
    }

    actual fun startSession(gameId: Long, emulatorPackage: String, coreName: String?, isHardcore: Boolean, isNewGame: Boolean, isNetplayGuest: Boolean, variantFileId: Long?, origin: LaunchOrigin) {
    }

    actual fun updateSessionHardcore(gameId: Long, isHardcore: Boolean) {
    }

    actual suspend fun endSession(stopService: Boolean, skipSaveSync: Boolean): SessionEndResult = SessionEndResult.Skipped

    actual fun sawSaveActivity(): Boolean = false

    actual fun markSaveObserved() {
    }

    actual val saveWrites: kotlinx.coroutines.flow.SharedFlow<Unit> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual fun reportSaveWritten() {
    }

    actual fun getSessionDuration(): Duration? = null

    actual fun cancelSession() {
    }

    actual fun endSessionInBackground(skipSaveSync: Boolean, stopService: Boolean): Job = kotlinx.coroutines.Job().apply { complete() }

    actual suspend fun cacheCurrentSessionForQuit(): SaveCacheManager.CacheResult? = null

    actual suspend fun cacheCurrentSessionStatesForQuit() {
    }

    actual fun forceStopService() {
    }

    actual fun canResumeSession(gameId: Long, variantFileId: Long?): Boolean = false
}
