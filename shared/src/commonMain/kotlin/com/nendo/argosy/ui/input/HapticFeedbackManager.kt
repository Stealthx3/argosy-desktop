package com.nendo.argosy.ui.input

expect class HapticFeedbackManager {
    fun vibrate(pattern: HapticPattern)

    fun setEnabled(enabled: Boolean)

    fun setStrength(strength: Float)
}
