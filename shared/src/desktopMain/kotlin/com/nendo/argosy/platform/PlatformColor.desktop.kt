package com.nendo.argosy.platform

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

actual object PlatformColor {
    actual val BLACK: Int get() = 0xFF000000.toInt()
    actual val WHITE: Int get() = 0xFFFFFFFF.toInt()
    actual val TRANSPARENT: Int get() = 0
    actual fun alpha(color: Int): Int = color ushr 24
    actual fun red(color: Int): Int = (color shr 16) and 0xFF
    actual fun green(color: Int): Int = (color shr 8) and 0xFF
    actual fun blue(color: Int): Int = color and 0xFF
    actual fun rgb(red: Int, green: Int, blue: Int): Int = argb(0xFF, red, green, blue)
    actual fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    actual fun parseColor(colorString: String): Int {
        require(colorString.startsWith("#")) { "Unknown color" }
        val value = colorString.substring(1).toLong(16)
        return when (colorString.length) {
            7 -> (value or 0xFF000000L).toInt()
            9 -> value.toInt()
            else -> throw IllegalArgumentException("Unknown color")
        }
    }

    actual fun colorToHSV(color: Int, hsv: FloatArray) {
        val r = red(color) / 255f
        val g = green(color) / 255f
        val b = blue(color) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        var h = when {
            delta == 0f -> 0f
            maxC == r -> 60f * (((g - b) / delta) % 6f)
            maxC == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        if (h < 0f) h += 360f
        hsv[0] = h
        hsv[1] = if (maxC == 0f) 0f else delta / maxC
        hsv[2] = maxC
    }

    actual fun HSVToColor(hsv: FloatArray): Int = HSVToColor(0xFF, hsv)

    actual fun HSVToColor(alpha: Int, hsv: FloatArray): Int {
        val h = ((hsv[0] % 360f) + 360f) % 360f
        val s = hsv[1].coerceIn(0f, 1f)
        val v = hsv[2].coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r, g, b) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun channel(f: Float) = ((f + m) * 255f).roundToInt().coerceIn(0, 255)
        return argb(alpha, channel(r), channel(g), channel(b))
    }
}
