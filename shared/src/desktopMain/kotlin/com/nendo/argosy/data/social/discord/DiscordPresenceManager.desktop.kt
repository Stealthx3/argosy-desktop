package com.nendo.argosy.data.social.discord

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class DiscordPresenceManager @Inject constructor() {
    actual companion object {}

    actual val state: StateFlow<DiscordPresenceState> = TODO("desktop: state")
}
