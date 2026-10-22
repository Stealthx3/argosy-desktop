package com.nendo.argosy.data.quaypass

import com.nendo.argosy.data.local.entity.GameEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class QuayPassService @Inject constructor() {

    actual companion object {
        actual fun getInstance(): QuayPassService? = null
    }

    actual val runState: StateFlow<QuayPassRunState> = kotlinx.coroutines.flow.MutableStateFlow(QuayPassRunState.DISABLED)

    actual fun isBleSupported(): Boolean = false

    actual fun isBleEnabled(): Boolean = false

    actual suspend fun holdForAccountSwitch() {
    }

    actual suspend fun releaseAccountSwitchHold() {
    }

    actual fun updateLastGame(game: GameEntity) {
    }
}
