package com.nendo.argosy.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

actual class PlatformConfiguration(
    val widthDp: Int,
    val heightDp: Int,
) {
    val orientationValue: Int get() = if (widthDp >= heightDp) ORIENTATION_LANDSCAPE else ORIENTATION_PORTRAIT

    companion object {
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
    }
}

actual val PlatformConfiguration.screenWidthDp: Int get() = widthDp

actual val PlatformConfiguration.screenHeightDp: Int get() = heightDp

actual val PlatformConfiguration.orientation: Int get() = orientationValue

actual class PlatformLocaleList(internal val items: List<java.util.Locale>)

actual val PlatformConfiguration.locales: PlatformLocaleList get() = PlatformLocaleList(listOf(java.util.Locale.getDefault()))

actual operator fun PlatformLocaleList.get(index: Int): java.util.Locale = items[index]

actual val LocalPlatformConfiguration: ProvidableCompositionLocal<PlatformConfiguration> =
    staticCompositionLocalOf { PlatformConfiguration(widthDp = 960, heightDp = 540) }

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
}

actual object PlatformColorUtils {
    actual fun colorToHSL(color: Int, outHsl: FloatArray) {
        val rf = ((color shr 16) and 0xFF) / 255f
        val gf = ((color shr 8) and 0xFF) / 255f
        val bf = (color and 0xFF) / 255f
        val maxC = max(rf, max(gf, bf))
        val minC = min(rf, min(gf, bf))
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        var h: Float
        val s: Float
        if (maxC == minC) {
            h = 0f
            s = 0f
        } else {
            h = when (maxC) {
                rf -> ((gf - bf) / delta) % 6f
                gf -> ((bf - rf) / delta) + 2f
                else -> ((rf - gf) / delta) + 4f
            }
            s = delta / (1f - abs(2f * l - 1f))
        }
        h = (h * 60f) % 360f
        if (h < 0) h += 360f
        outHsl[0] = h.coerceIn(0f, 360f)
        outHsl[1] = s.coerceIn(0f, 1f)
        outHsl[2] = l.coerceIn(0f, 1f)
    }

    actual fun HSLToColor(hsl: FloatArray): Int {
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]
        val c = (1f - abs(2 * l - 1f)) * s
        val m = l - 0.5f * c
        val x = c * (1f - abs((h / 60f % 2f) - 1f))
        val (r, g, b) = when (h.toInt() / 60) {
            0 -> Triple(c + m, x + m, m)
            1 -> Triple(x + m, c + m, m)
            2 -> Triple(m, c + m, x + m)
            3 -> Triple(m, x + m, c + m)
            4 -> Triple(x + m, m, c + m)
            5, 6 -> Triple(c + m, m, x + m)
            else -> Triple(0f, 0f, 0f)
        }
        fun channel(v: Float) = (255 * v).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}
