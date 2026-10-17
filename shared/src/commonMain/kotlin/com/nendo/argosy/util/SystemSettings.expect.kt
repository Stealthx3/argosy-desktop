package com.nendo.argosy.util

expect class SystemSettings {
    fun screenBrightness(): Float?

    fun setScreenBrightness(fraction: Float): Boolean
}
