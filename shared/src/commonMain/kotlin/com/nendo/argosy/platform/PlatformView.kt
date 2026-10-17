package com.nendo.argosy.platform

import androidx.compose.runtime.ProvidableCompositionLocal

expect open class PlatformView

expect fun PlatformView.getLocationOnScreen(outLocation: IntArray)

expect val LocalPlatformView: ProvidableCompositionLocal<PlatformView>
