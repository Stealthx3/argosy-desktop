package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.local.entity.EmulatorUpdateEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class EmulatorUpdateManager @Inject constructor() {
    actual companion object {}

    actual val checkState: StateFlow<UpdateCheckState> = kotlinx.coroutines.flow.MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)

    actual val updateCount: Flow<Int> = kotlinx.coroutines.flow.flowOf(0)

    actual val availableUpdates: Flow<List<EmulatorUpdateEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())

    actual val assignedUpdateCount: Flow<Int> = kotlinx.coroutines.flow.flowOf(0)

    actual val platformUpdateCounts: Flow<Map<String, Int>> = kotlinx.coroutines.flow.flowOf(emptyMap())

    actual fun checkIfNeeded() {
    }

    actual fun forceCheck() {
    }

    actual suspend fun checkForUpdates(ignoreCache: Boolean): UpdateCheckState = UpdateCheckState.Idle

    actual suspend fun getUpdateForEmulator(emulatorId: String): EmulatorUpdateEntity? = null

    actual suspend fun getUpdatesForPlatform(platformSlug: String): List<EmulatorUpdateEntity> = emptyList()

    actual suspend fun hasUpdateForPlatform(platformSlug: String): Boolean = false

    actual suspend fun markAsInstalled(emulatorId: String, version: String, variant: String?) {
    }

    actual fun resetCheckState() {
    }
}
