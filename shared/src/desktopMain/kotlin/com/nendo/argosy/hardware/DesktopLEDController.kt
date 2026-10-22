package com.nendo.argosy.hardware

import androidx.compose.ui.graphics.Color
import javax.inject.Inject

class DesktopLEDController @Inject constructor() : LEDController {
    override val isAvailable: Boolean = false

    override fun setColor(left: Color, right: Color): Boolean = false

    override fun setBrightness(percent: Float): Boolean = false

    override fun setEnabled(left: Boolean, right: Boolean): Boolean = false
}
