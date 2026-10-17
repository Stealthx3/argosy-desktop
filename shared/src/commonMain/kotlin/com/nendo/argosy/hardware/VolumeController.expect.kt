package com.nendo.argosy.hardware

expect class VolumeController {
    val maxVolume: Int

    fun getVolume(): DisplayVolume

    fun setPrimaryVolume(volume: Float): Boolean
}
