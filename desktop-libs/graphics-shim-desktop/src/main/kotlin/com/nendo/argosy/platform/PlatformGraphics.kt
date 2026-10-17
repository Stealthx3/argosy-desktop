package com.nendo.argosy.platform

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Shader

enum class PlatformBlur {
    NORMAL,
    SOLID,
    OUTER,
    INNER
}

enum class PlatformTileMode {
    CLAMP,
    REPEAT,
    MIRROR
}

private const val BLUR_SIGMA_SCALE = 0.57735f
private const val BLUR_SIGMA_OFFSET = 0.5f

fun PlatformNativePaint(): Paint = Paint()

fun PlatformBlurMaskFilter(radius: Float, style: PlatformBlur): MaskFilter {
    val mode = when (style) {
        PlatformBlur.NORMAL -> FilterBlurMode.NORMAL
        PlatformBlur.SOLID -> FilterBlurMode.SOLID
        PlatformBlur.OUTER -> FilterBlurMode.OUTER
        PlatformBlur.INNER -> FilterBlurMode.INNER
    }
    return MaskFilter.makeBlur(mode, if (radius > 0f) radius * BLUR_SIGMA_SCALE + BLUR_SIGMA_OFFSET else 0f)
}

private fun PlatformTileMode.toSkia(): FilterTileMode = when (this) {
    PlatformTileMode.CLAMP -> FilterTileMode.CLAMP
    PlatformTileMode.REPEAT -> FilterTileMode.REPEAT
    PlatformTileMode.MIRROR -> FilterTileMode.MIRROR
}

fun PlatformLinearGradient(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    color0: Int,
    color1: Int,
    tile: PlatformTileMode,
): Shader = PlatformLinearGradient(x0, y0, x1, y1, intArrayOf(color0, color1), null, tile)

fun PlatformLinearGradient(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    colors: IntArray,
    positions: FloatArray?,
    tile: PlatformTileMode,
): Shader = Shader.makeLinearGradient(Point(x0, y0), Point(x1, y1), colors, positions, GradientStyle(tile.toSkia(), true, null))

fun Canvas.drawRoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    rx: Float,
    ry: Float,
    paint: Paint,
) {
    drawRRect(RRect.makeLTRB(left, top, right, bottom, rx, ry), paint)
}
