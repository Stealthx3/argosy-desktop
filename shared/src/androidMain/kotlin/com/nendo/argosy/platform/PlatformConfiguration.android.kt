package com.nendo.argosy.platform

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.graphics.ColorUtils

actual typealias PlatformConfiguration = Configuration

actual val PlatformConfiguration.screenWidthDp: Int get() = screenWidthDp

actual val PlatformConfiguration.screenHeightDp: Int get() = screenHeightDp

actual val PlatformConfiguration.orientation: Int get() = orientation

actual typealias PlatformLocaleList = LocaleList

actual val PlatformConfiguration.locales: PlatformLocaleList get() = locales

actual operator fun PlatformLocaleList.get(index: Int): java.util.Locale = get(index)

actual val LocalPlatformConfiguration: ProvidableCompositionLocal<PlatformConfiguration> = LocalConfiguration

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled, onBack)
}

actual object PlatformColorUtils {
    actual fun colorToHSL(color: Int, outHsl: FloatArray) = ColorUtils.colorToHSL(color, outHsl)
    actual fun HSLToColor(hsl: FloatArray): Int = ColorUtils.HSLToColor(hsl)
}
