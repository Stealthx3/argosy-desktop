package com.nendo.argosy.data.remote.jellyfin

expect class JellyfinDeviceProfileBuilder {
    companion object {
        val DEFAULT_MAX_AUDIO_CHANNELS: Int
    }

    fun build(maxStreamingBitrateKbps: Int? = null, maxHeight: Int? = null, maxFramerate: Int? = null, burnInImageSubtitles: Boolean = false, maxAudioChannels: Int = 6): JellyfinDeviceProfile

    fun burnInSubtitleProfiles(): List<JellyfinSubtitleProfile>
}
