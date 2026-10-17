package com.nendo.argosy.ui.input

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class HapticFeedbackManager @Inject constructor() {
    actual fun vibrate(pattern: HapticPattern) {
    }

    actual fun setEnabled(enabled: Boolean) {
    }

    actual fun setStrength(strength: Float) {
    }
}
