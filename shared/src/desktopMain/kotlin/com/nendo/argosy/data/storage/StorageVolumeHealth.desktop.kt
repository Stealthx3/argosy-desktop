package com.nendo.argosy.data.storage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class StorageVolumeHealth @Inject constructor() {

    actual fun newProbe(): VolumeProbe = TODO("desktop: newProbe")
}
