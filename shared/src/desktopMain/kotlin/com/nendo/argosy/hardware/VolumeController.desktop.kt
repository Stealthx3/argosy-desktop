package com.nendo.argosy.hardware

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class VolumeController @Inject constructor() {
    actual val maxVolume: Int = 0

    actual fun getVolume(): DisplayVolume = DisplayVolume(primary = 0f, secondary = null)

    actual fun setPrimaryVolume(volume: Float): Boolean = false
}
