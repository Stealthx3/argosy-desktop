package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.SoundType

expect class SoundFeedbackManager {
    fun playSound(type: SoundType)

    fun play(type: SoundType)
}
