package com.nendo.argosy.platform

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.MaskFilter
import android.graphics.Paint
import android.graphics.Shader

typealias PlatformBlur = BlurMaskFilter.Blur

typealias PlatformTileMode = Shader.TileMode

fun PlatformNativePaint(): Paint = Paint()

fun PlatformBlurMaskFilter(radius: Float, style: PlatformBlur): MaskFilter = BlurMaskFilter(radius, style)

fun PlatformLinearGradient(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    color0: Int,
    color1: Int,
    tile: PlatformTileMode,
): Shader = LinearGradient(x0, y0, x1, y1, color0, color1, tile)

fun PlatformLinearGradient(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    colors: IntArray,
    positions: FloatArray?,
    tile: PlatformTileMode,
): Shader = LinearGradient(x0, y0, x1, y1, colors, positions, tile)

fun Canvas.drawRoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    rx: Float,
    ry: Float,
    paint: Paint,
) = drawRoundRect(left, top, right, bottom, rx, ry, paint)
