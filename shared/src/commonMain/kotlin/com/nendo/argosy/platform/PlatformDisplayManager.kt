package com.nendo.argosy.platform

const val DISPLAY_SERVICE: String = "display"

const val DEFAULT_DISPLAY: Int = 0

expect class PlatformDisplayManager

expect fun PlatformDisplayManager.getDisplay(displayId: Int): PlatformDisplay?

expect class PlatformDisplayMode

expect val PlatformDisplay.supportedModes: Array<PlatformDisplayMode>

expect val PlatformDisplayMode.refreshRate: Float
