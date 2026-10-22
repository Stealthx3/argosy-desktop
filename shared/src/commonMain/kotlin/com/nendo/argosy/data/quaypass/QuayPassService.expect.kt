package com.nendo.argosy.data.quaypass

import com.nendo.argosy.data.local.entity.GameEntity
import kotlinx.coroutines.flow.StateFlow

expect class QuayPassService {
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
