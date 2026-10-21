package com.nendo.argosy.platform

import java.awt.DisplayMode
import java.awt.GraphicsEnvironment

actual class PlatformDisplayManager

internal val DesktopDisplayManager = PlatformDisplayManager()

actual fun PlatformDisplayManager.getDisplay(displayId: Int): PlatformDisplay? =
    if (displayId == DEFAULT_DISPLAY) PlatformDisplay(displayId) else null

actual class PlatformDisplayMode(val refreshRateHz: Float)

actual val PlatformDisplay.supportedModes: Array<PlatformDisplayMode>
    get() = runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayModes
            .map(DisplayMode::getRefreshRate)
            .filter { it != DisplayMode.REFRESH_RATE_UNKNOWN }
            .map { PlatformDisplayMode(it.toFloat()) }
            .toTypedArray()
    }.getOrDefault(emptyArray())

actual val PlatformDisplayMode.refreshRate: Float get() = refreshRateHz
