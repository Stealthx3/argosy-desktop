package com.nendo.argosy.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal

expect class PlatformConfiguration

expect val PlatformConfiguration.screenWidthDp: Int

expect val PlatformConfiguration.screenHeightDp: Int

expect val PlatformConfiguration.orientation: Int

expect val LocalPlatformConfiguration: ProvidableCompositionLocal<PlatformConfiguration>

@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

expect object PlatformColorUtils {
    fun colorToHSL(color: Int, outHsl: FloatArray)
    fun HSLToColor(hsl: FloatArray): Int
}

const val ORIENTATION_UNDEFINED: Int = 0

const val ORIENTATION_PORTRAIT: Int = 1

const val ORIENTATION_LANDSCAPE: Int = 2

const val ORIENTATION_SQUARE: Int = 3

const val UI_MODE_NIGHT_NO: Int = 16

const val UI_MODE_NIGHT_YES: Int = 32

const val UI_MODE_NIGHT_MASK: Int = 48
