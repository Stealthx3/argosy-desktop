package com.nendo.argosy.hardware

import com.nendo.argosy.util.SystemSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The built-in panel's backlight through [SystemSettings]. A desktop has no second built-in panel.
 */
@Singleton
actual class BrightnessController @Inject constructor(
    private val systemSettings: SystemSettings
) {
    actual companion object {}

    actual fun getBrightness(): DisplayBrightness =
        DisplayBrightness(primary = systemSettings.screenBrightness(), secondary = null)

    actual fun setPrimaryBrightness(brightness: Float): Boolean =
        systemSettings.setScreenBrightness(brightness)

    actual fun secondaryBrightness(): Float? = null

    actual fun setSecondaryBrightness(brightness: Float): Boolean = false
}
