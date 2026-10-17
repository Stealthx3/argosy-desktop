package com.nendo.argosy.data.quaypass

import com.nendo.argosy.data.local.entity.GameEntity
import kotlinx.coroutines.flow.StateFlow

expect class QuayPassService {
    enum class QuayPassRunState { RUNNING, DISABLED, NOT_LINKED, BLE_UNSUPPORTED, BLUETOOTH_OFF, PERMISSIONS_MISSING, AWAITING_REGISTRATION, AWAITING_CREDENTIAL, KEY_EXPIRED, CREDENTIAL_REJECTED }

    companion object {
        fun getInstance(): QuayPassService?
    }

    val runState: StateFlow<QuayPassRunState>

    fun isBleSupported(): Boolean

    fun isBleEnabled(): Boolean

    suspend fun holdForAccountSwitch()

    suspend fun releaseAccountSwitchHold()

    fun updateLastGame(game: GameEntity)
}
