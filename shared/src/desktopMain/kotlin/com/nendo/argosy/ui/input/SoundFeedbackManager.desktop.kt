package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.SoundConfig
import com.nendo.argosy.core.input.SoundType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class SoundFeedbackManager @Inject constructor() {
    actual fun playSound(type: SoundType) {
    }

    actual fun play(type: SoundType) {
    }

    actual fun setEnabled(enabled: Boolean) {
    }

    actual suspend fun clearSfxCache() {
    }

    actual fun setVolume(volume: Int) {
    }

    actual fun setSoundConfig(type: SoundType, config: SoundConfig) {
    }

    actual fun clearSoundConfig(type: SoundType) {
    }
}
