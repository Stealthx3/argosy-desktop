package com.nendo.argosy.data.storage

import com.nendo.argosy.core.storage.StorageVolume as AppStorageVolume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class StorageVolumeDetector @Inject constructor() {
    actual fun detectStorageVolumes(): List<AppStorageVolume> = emptyList()

    actual fun volumeRootPaths(): List<String> = emptyList()
}
