package com.nendo.argosy.ui.audio

import com.nendo.argosy.domain.model.MusicQueueTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class AmbientAudioManager @Inject constructor() {
    actual companion object {
        actual const val AMBIENT_SOURCE_PLAYLIST: String = "playlist:"
    }

    actual val currentTrackName: StateFlow<String?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val playback: StateFlow<AmbientPlaybackState> = kotlinx.coroutines.flow.MutableStateFlow(AmbientPlaybackState())

    actual fun setEnabled(enabled: Boolean) {
    }

    actual val currentVolume: Float = 0f

    actual fun setVolume(volume: Int) {
    }

    actual fun setShuffle(shuffle: Boolean) {
    }

    actual fun setPlayInBackground(enabled: Boolean) {
    }

    actual fun keepsPlayingAsleep(): Boolean = false

    actual fun onLauncherWindowFocused(window: String) {
    }

    actual fun onLauncherWindowUnfocused(window: String) {
    }

    actual fun setLauncherSource(tracks: List<MusicQueueTrack>, refresh: (suspend () -> List<MusicQueueTrack>)?) {
    }

    actual fun playSelection(sourceId: String, sourceLabel: String?, tracks: List<MusicQueueTrack>) {
    }

    actual fun activateLauncher() {
    }

    actual fun play() {
    }

    actual fun pause() {
    }

    actual fun skipNext() {
    }

    actual fun skipPrevious() {
    }

    actual fun playAt(position: Int) {
    }

    actual fun positionSnapshot(): PlaybackPosition? = null

    actual suspend fun playOverride(source: AmbientOverrideSource) {
    }

    actual fun clearOverride() {
    }

    actual fun suspend() {
    }

    actual fun resumeFromSuspend() {
    }

    actual fun holdSilence() {
    }

    actual fun releaseSilence() {
    }

    actual fun holdVideoSilence() {
    }

    actual fun releaseVideoSilence() {
    }

    actual fun fadeIn(durationMs: Long) {
    }

    actual fun fadeOut(durationMs: Long, onComplete: () -> Unit) {
    }

    actual fun release() {
    }
}
