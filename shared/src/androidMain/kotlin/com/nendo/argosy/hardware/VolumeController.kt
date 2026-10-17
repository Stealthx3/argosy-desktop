package com.nendo.argosy.hardware

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
actual class VolumeController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    actual val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    actual fun getVolume(): DisplayVolume {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return DisplayVolume(primary = current.toFloat() / max.toFloat(), secondary = null)
    }

    actual fun setPrimaryVolume(volume: Float): Boolean {
        val clamped = volume.coerceIn(0f, 1f)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val value = (clamped * max).roundToInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
        return true
    }
}
