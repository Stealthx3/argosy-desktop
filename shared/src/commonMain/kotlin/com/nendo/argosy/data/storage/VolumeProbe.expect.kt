package com.nendo.argosy.data.storage

expect class VolumeProbe {
    fun isVolumeReadable(path: String): Boolean

    fun isGenuinelyAbsent(path: String): Boolean

    fun allVolumesReadable(paths: Collection<String>): Boolean
}
