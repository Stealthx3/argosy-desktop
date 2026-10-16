package androidx.core.graphics

/** androidx.core ColorUtils HSL conversions (same math as the AndroidX implementation). */
object ColorUtils {
    fun colorToHSL(color: Int, outHsl: FloatArray) {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val max = maxOf(r, g, b); val min = minOf(r, g, b); val delta = max - min
        val l = (max + min) / 2f
        var h: Float; val s: Float
        if (max == min) { h = 0f; s = 0f } else {
            h = when (max) {
                r -> ((g - b) / delta) % 6f
                g -> (b - r) / delta + 2f
                else -> (r - g) / delta + 4f
            }
            s = delta / (1f - kotlin.math.abs(2f * l - 1f))
        }
        h = (h * 60f) % 360f
        if (h < 0) h += 360f
        outHsl[0] = h.coerceIn(0f, 360f); outHsl[1] = s.coerceIn(0f, 1f); outHsl[2] = l.coerceIn(0f, 1f)
    }

    fun HSLToColor(hsl: FloatArray): Int {
        val h = hsl[0]; val s = hsl[1]; val l = hsl[2]
        val c = (1f - kotlin.math.abs(2 * l - 1f)) * s
        val m = l - 0.5f * c
        val x = c * (1f - kotlin.math.abs((h / 60f % 2f) - 1f))
        val hueSegment = h.toInt() / 60
        var r = 0; var g = 0; var b = 0
        fun f(v: Float) = Math.round(255 * (v + m)).coerceIn(0, 255)
        when (hueSegment) {
            0 -> { r = f(c); g = f(x); b = f(0f) }
            1 -> { r = f(x); g = f(c); b = f(0f) }
            2 -> { r = f(0f); g = f(c); b = f(x) }
            3 -> { r = f(0f); g = f(x); b = f(c) }
            4 -> { r = f(x); g = f(0f); b = f(c) }
            else -> { r = f(c); g = f(0f); b = f(x) }
        }
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
