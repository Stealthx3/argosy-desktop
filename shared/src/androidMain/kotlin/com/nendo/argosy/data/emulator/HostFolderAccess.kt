package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.model.EmulatorFolderAccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class HostFolderAccess @Inject constructor() {
    actual val isSupported: Boolean = false

    actual suspend fun emulators(folder: String): List<EmulatorFolderAccess> = emptyList()

    actual suspend fun grant(appId: String, folder: String): Boolean = false
}
