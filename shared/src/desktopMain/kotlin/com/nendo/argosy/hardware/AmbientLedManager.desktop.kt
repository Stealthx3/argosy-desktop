package com.nendo.argosy.hardware

import androidx.compose.ui.graphics.Color
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class AmbientLedManager @Inject constructor() {
    actual fun setUiColors(primary: Color, secondary: Color?) {
    }

    actual fun setContext(newContext: AmbientLedContext) {
    }

    actual fun setHoverColors(primary: Color, secondary: Color?) {
    }

    actual fun clearHoverColors() {
    }
}
