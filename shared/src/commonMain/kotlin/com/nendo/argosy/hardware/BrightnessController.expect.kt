package com.nendo.argosy.hardware

expect class BrightnessController {
    companion object {}

    fun getBrightness(): DisplayBrightness

    fun setPrimaryBrightness(brightness: Float): Boolean

    fun secondaryBrightness(): Float?

    fun setSecondaryBrightness(brightness: Float): Boolean
}
