package com.nendo.argosy.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

/**
 * Desktop actual of upstream's Modifier.focusGlow (ui/util/FocusModifiers.kt). Same drawing; the
 * blurred circle goes through Skia's MaskFilter instead of android.graphics.BlurMaskFilter.
 */
fun Modifier.focusGlow(
    color: Color,
    blurRadius: Float = 16f,
    spread: Float = 8f
): Modifier = if (color.alpha == 0f) this else drawBehind {
    drawIntoCanvas { canvas ->
        val paint = org.jetbrains.skia.Paint().apply {
            this.color = color.toArgb()
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurRadius / 2f)
        }
        val radius = size.minDimension / 2
        canvas.nativeCanvas.drawCircle(size.width / 2, size.height / 2, radius + spread, paint)
    }
}
