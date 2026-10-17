package com.nendo.argosy.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

actual open class PlatformView(
    val screenX: Int = 0,
    val screenY: Int = 0,
)

actual fun PlatformView.getLocationOnScreen(outLocation: IntArray) {
    outLocation[0] = screenX
    outLocation[1] = screenY
}

actual val LocalPlatformView: ProvidableCompositionLocal<PlatformView> = staticCompositionLocalOf { PlatformView() }
