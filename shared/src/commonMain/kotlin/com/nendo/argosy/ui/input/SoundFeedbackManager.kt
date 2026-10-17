package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.SoundConfig
import com.nendo.argosy.core.input.SoundType

expect class SoundFeedbackManager {
    fun playSound(type: SoundType)

    fun play(type: SoundType)

    fun setEnabled(enabled: Boolean)

    suspend fun clearSfxCache()

    fun setVolume(volume: Int)

    fun setSoundConfig(type: SoundType, config: SoundConfig)

    fun clearSoundConfig(type: SoundType)
}
