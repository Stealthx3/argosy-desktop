package com.nendo.argosy.data.emulator

import java.time.Instant

data class ActiveSession(
    val gameId: Long,
    val startTime: Instant,
    val emulatorPackage: String,
    val coreName: String? = null,
    val isHardcore: Boolean = false,
    val isNewGame: Boolean = false,
    val channelName: String? = null,
    val isNetplayGuest: Boolean = false,
    val variantFileId: Long? = null,
    val origin: LaunchOrigin = LaunchOrigin.INTERNAL
)

sealed class SessionEndResult {
    data object Success : SessionEndResult()
    data object Duplicate : SessionEndResult()
    data object Skipped : SessionEndResult()
    data class SaveUnreadable(val dirPath: String, val emulatorId: String) : SessionEndResult()
    data class Error(val message: String) : SessionEndResult()
}

data class SaveConflictEvent(
    val gameId: Long,
    val emulatorId: String,
    val channelName: String?,
    val localTimestamp: Instant,
    val serverTimestamp: Instant,
    val serverDeviceName: String? = null,
    val conflictId: Long? = null,
    val isHardcoreDowngrade: Boolean = false
)
