package com.nendo.argosy.hardware

import androidx.compose.ui.graphics.Color

expect class AmbientLedManager {
    fun setUiColors(primary: Color, secondary: Color?)

    fun setContext(newContext: AmbientLedContext)

    fun setHoverColors(primary: Color, secondary: Color?)

    fun clearHoverColors()
}
