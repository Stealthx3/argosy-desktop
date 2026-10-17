package com.nendo.argosy.platform

import android.view.View
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.platform.LocalView

actual typealias PlatformView = View

actual fun PlatformView.getLocationOnScreen(outLocation: IntArray) = getLocationOnScreen(outLocation)

actual val LocalPlatformView: ProvidableCompositionLocal<PlatformView> = LocalView
