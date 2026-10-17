package com.nendo.argosy.data.steam

enum class SteamConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LOGGING_IN,
    LOGGED_IN,
    LOGGED_OUT
}

data class SteamServiceState(
    val connectionState: SteamConnectionState = SteamConnectionState.DISCONNECTED,
    val username: String? = null,
    val steamId: Long? = null,
    val error: String? = null
)
