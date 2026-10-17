package com.nendo.argosy.data.steam

import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class SteamService {
    companion object {
        val EXTRA_AUTO_CONNECT: String

        val EXTRA_FORCE_CONNECT: String

        val EXTRA_CONNECT_FOR_AUTH: String
    }

    val state: StateFlow<SteamServiceState>

    fun connect()

    fun disconnect()

    fun getSteamClient(): SteamClient?

    fun getSteamUser(): SteamUser?

    fun getSteamApps(): SteamApps?

    fun getCallbackManager(): CallbackManager?

    fun getContentManager(): SteamContentManager

    fun getLibraryManager(): SteamLibraryManager
}
