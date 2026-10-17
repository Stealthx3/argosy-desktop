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
