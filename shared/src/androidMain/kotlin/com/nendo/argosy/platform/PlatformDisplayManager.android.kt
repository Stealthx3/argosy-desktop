package com.nendo.argosy.platform

import android.hardware.display.DisplayManager
import android.view.Display

actual typealias PlatformDisplayManager = DisplayManager

actual fun PlatformDisplayManager.getDisplay(displayId: Int): PlatformDisplay? = getDisplay(displayId)

actual typealias PlatformDisplayMode = Display.Mode

actual val PlatformDisplay.supportedModes: Array<PlatformDisplayMode> get() = supportedModes

actual val PlatformDisplayMode.refreshRate: Float get() = refreshRate
