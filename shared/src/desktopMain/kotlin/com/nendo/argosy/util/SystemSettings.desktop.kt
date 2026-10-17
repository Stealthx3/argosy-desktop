package com.nendo.argosy.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class SystemSettings @Inject constructor() {
    actual fun screenBrightness(): Float? = null

    actual fun setScreenBrightness(fraction: Float): Boolean = false
}
