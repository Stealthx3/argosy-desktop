package com.nendo.argosy.ui.input

import com.nendo.argosy.platform.sdl.SdlGamepadSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The running SDL gamepad source, as far as haptics need it: [DesktopGamepadDispatcher] attaches
 * it on start, and [HapticFeedbackManager] rumbles through it.
 */
@Singleton
class GamepadRumble @Inject constructor() {
    @Volatile
    private var source: SdlGamepadSource? = null

    fun attach(source: SdlGamepadSource) {
        this.source = source
    }

    fun rumble(low: Int, high: Int, durationMs: Int): Boolean =
        source?.rumble(low, high, durationMs) == true
}
