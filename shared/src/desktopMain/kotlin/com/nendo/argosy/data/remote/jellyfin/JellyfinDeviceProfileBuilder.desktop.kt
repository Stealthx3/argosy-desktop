package com.nendo.argosy.data.remote.jellyfin

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class JellyfinDeviceProfileBuilder @Inject constructor() {
    actual companion object {
        actual const val DEFAULT_MAX_AUDIO_CHANNELS: Int = 6
    }

    actual fun build(maxStreamingBitrateKbps: Int?, maxHeight: Int?, maxFramerate: Int?, burnInImageSubtitles: Boolean, maxAudioChannels: Int): JellyfinDeviceProfile = JellyfinDeviceProfile()

    actual fun burnInSubtitleProfiles(): List<JellyfinSubtitleProfile> = emptyList()
}
