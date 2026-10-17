package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.repository.SaveCacheManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration

expect class PlaySessionTracker {
    companion object {}

    val activeSession: StateFlow<ActiveSession?>

    val hasActiveSession: StateFlow<Boolean>

    val pendingSessionConflict: StateFlow<SaveConflictEvent?>

    fun clearPendingSessionConflict()

    val externalSessionClosed: SharedFlow<Unit>

    suspend fun checkOrphanedSession()

    fun startSession(gameId: Long, emulatorPackage: String, coreName: String? = null, isHardcore: Boolean = false, isNewGame: Boolean = false, isNetplayGuest: Boolean = false, variantFileId: Long? = null, origin: LaunchOrigin = LaunchOrigin.INTERNAL)

    fun updateSessionHardcore(gameId: Long, isHardcore: Boolean)

    suspend fun endSession(stopService: Boolean = true, skipSaveSync: Boolean = false): SessionEndResult

    fun sawSaveActivity(): Boolean

    fun markSaveObserved()

    val saveWrites: kotlinx.coroutines.flow.SharedFlow<Unit>

    fun reportSaveWritten()

    fun getSessionDuration(): Duration?

    fun cancelSession()

    fun endSessionInBackground(skipSaveSync: Boolean = false, stopService: Boolean = true): Job

    suspend fun cacheCurrentSessionForQuit(): SaveCacheManager.CacheResult?

    suspend fun cacheCurrentSessionStatesForQuit()

    fun forceStopService()

    fun canResumeSession(gameId: Long, variantFileId: Long? = null): Boolean
}
