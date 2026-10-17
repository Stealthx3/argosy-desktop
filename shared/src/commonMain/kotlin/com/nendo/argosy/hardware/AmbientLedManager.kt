package com.nendo.argosy.hardware

import androidx.compose.ui.graphics.Color

expect class AmbientLedManager {
    fun setUiColors(primary: Color, secondary: Color?)
}
