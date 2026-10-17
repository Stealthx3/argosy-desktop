package com.nendo.argosy.ui.audio

import com.nendo.argosy.domain.model.MusicQueueTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class AmbientAudioManager {
    companion object {
        val AMBIENT_SOURCE_PLAYLIST: String
    }

    val currentTrackName: StateFlow<String?>

    val playback: StateFlow<AmbientPlaybackState>

    fun setEnabled(enabled: Boolean)

    val currentVolume: Float

    fun setVolume(volume: Int)

    fun setShuffle(shuffle: Boolean)

    fun setPlayInBackground(enabled: Boolean)

    fun keepsPlayingAsleep(): Boolean

    fun onLauncherWindowFocused(window: String)

    fun onLauncherWindowUnfocused(window: String)

    fun setLauncherSource(tracks: List<MusicQueueTrack>, refresh: (suspend () -> List<MusicQueueTrack>)? = null)

    fun playSelection(sourceId: String, sourceLabel: String?, tracks: List<MusicQueueTrack>)

    fun activateLauncher()

    fun play()

    fun pause()

    fun skipNext()

    fun skipPrevious()

    fun playAt(position: Int)

    fun positionSnapshot(): PlaybackPosition?

    suspend fun playOverride(source: AmbientOverrideSource)

    fun clearOverride()

    fun suspend()

    fun resumeFromSuspend()

    fun holdSilence()

    fun releaseSilence()

    fun holdVideoSilence()

    fun releaseVideoSilence()

    fun fadeIn(durationMs: Long = 500)

    fun fadeOut(durationMs: Long = 500, onComplete: () -> Unit = {})

    fun release()
}
