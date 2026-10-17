package com.nendo.argosy.data.steam

import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class SteamService @Inject constructor() {
    actual companion object {
        actual const val EXTRA_AUTO_CONNECT: String = "auto_connect"

        actual const val EXTRA_FORCE_CONNECT: String = "force_connect"

        actual const val EXTRA_CONNECT_FOR_AUTH: String = "connect_for_auth"
    }

    actual val state: StateFlow<SteamServiceState> = kotlinx.coroutines.flow.MutableStateFlow(SteamServiceState())

    actual fun connect() {
    }

    actual fun disconnect() {
    }

    actual fun getSteamClient(): SteamClient? = null

    actual fun getSteamUser(): SteamUser? = null

    actual fun getSteamApps(): SteamApps? = null

    actual fun getCallbackManager(): CallbackManager? = null

    actual fun getContentManager(): SteamContentManager = TODO("desktop: getContentManager")

    actual fun getLibraryManager(): SteamLibraryManager = TODO("desktop: getLibraryManager")
}
