package com.nendo.argosy.data.social.discord

import kotlinx.coroutines.flow.StateFlow

expect class DiscordPresenceManager {
    companion object {}

    val state: StateFlow<DiscordPresenceState>
}
