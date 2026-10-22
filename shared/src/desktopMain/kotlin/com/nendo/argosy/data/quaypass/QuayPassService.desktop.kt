package com.nendo.argosy.data.quaypass

import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
actual class QuayPassService @Inject constructor(
    preferencesRepository: UserPreferencesRepository
) {

    actual companion object {
        actual fun getInstance(): QuayPassService? = null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    actual val runState: StateFlow<QuayPassRunState> = preferencesRepository.userPreferences
        .map { if (it.socialSessionToken != null) QuayPassRunState.DISABLED else QuayPassRunState.NOT_LINKED }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, QuayPassRunState.DISABLED)

    actual fun isBleSupported(): Boolean = false

    actual fun isBleEnabled(): Boolean = false

    actual suspend fun holdForAccountSwitch() {
    }

    actual suspend fun releaseAccountSwitchHold() {
    }

    actual fun updateLastGame(game: GameEntity) {
    }
}
