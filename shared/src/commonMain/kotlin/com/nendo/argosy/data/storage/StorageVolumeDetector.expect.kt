package com.nendo.argosy.data.storage

import com.nendo.argosy.core.storage.StorageVolume as AppStorageVolume

expect class StorageVolumeDetector {
    fun detectStorageVolumes(): List<AppStorageVolume>

    fun volumeRootPaths(): List<String>
}
