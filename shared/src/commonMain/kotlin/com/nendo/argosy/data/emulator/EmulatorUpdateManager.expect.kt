package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.local.entity.EmulatorUpdateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class EmulatorUpdateManager {
    companion object {}

    val checkState: StateFlow<UpdateCheckState>

    val updateCount: Flow<Int>

    val availableUpdates: Flow<List<EmulatorUpdateEntity>>

    val assignedUpdateCount: Flow<Int>

    val platformUpdateCounts: Flow<Map<String, Int>>

    fun checkIfNeeded()

    fun forceCheck()

    suspend fun checkForUpdates(ignoreCache: Boolean = false): UpdateCheckState

    suspend fun getUpdateForEmulator(emulatorId: String): EmulatorUpdateEntity?

    suspend fun getUpdatesForPlatform(platformSlug: String): List<EmulatorUpdateEntity>

    suspend fun hasUpdateForPlatform(platformSlug: String): Boolean

    suspend fun markAsInstalled(emulatorId: String, version: String, variant: String?)

    fun resetCheckState()
}
