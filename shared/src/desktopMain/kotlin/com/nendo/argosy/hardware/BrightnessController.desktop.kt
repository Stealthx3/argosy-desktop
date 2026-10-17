package com.nendo.argosy.hardware

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class BrightnessController @Inject constructor() {
    actual companion object {}

    actual fun getBrightness(): DisplayBrightness = DisplayBrightness(primary = null, secondary = null)

    actual fun setPrimaryBrightness(brightness: Float): Boolean = false

    actual fun secondaryBrightness(): Float? = null

    actual fun setSecondaryBrightness(brightness: Float): Boolean = false
}
