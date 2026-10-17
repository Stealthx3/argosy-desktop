package com.nendo.argosy.data.quaypass

import com.nendo.argosy.data.local.entity.GameEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class QuayPassService @Inject constructor() {
    actual enum class QuayPassRunState { RUNNING, DISABLED, NOT_LINKED, BLE_UNSUPPORTED, BLUETOOTH_OFF, PERMISSIONS_MISSING, AWAITING_REGISTRATION, AWAITING_CREDENTIAL, KEY_EXPIRED, CREDENTIAL_REJECTED }

    actual companion object {
        actual fun getInstance(): QuayPassService? = null
    }

    actual val runState: StateFlow<QuayPassRunState> = TODO("desktop: runState")

    actual fun isBleSupported(): Boolean = false

    actual fun isBleEnabled(): Boolean = false

    actual suspend fun holdForAccountSwitch() {
    }

    actual suspend fun releaseAccountSwitchHold() {
    }

    actual fun updateLastGame(game: GameEntity) {
    }
}
