package com.nendo.argosy.data.storage

import javax.inject.Inject

actual class VolumeProbe @Inject constructor() {
    actual fun isVolumeReadable(path: String): Boolean = false

    actual fun isGenuinelyAbsent(path: String): Boolean = false

    actual fun allVolumesReadable(paths: Collection<String>): Boolean = false
}
