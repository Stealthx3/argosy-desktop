package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.model.EmulatorFolderAccess

/**
 * Lets sandboxed emulators open the folder Argosy keeps games in. Android emulators reach it
 * through shared storage, so Android has nothing to grant; the desktop grants each emulator
 * Flatpak a filesystem override.
 */
expect class HostFolderAccess {
    val isSupported: Boolean

    suspend fun emulators(folder: String): List<EmulatorFolderAccess>

    suspend fun grant(appId: String, folder: String): Boolean
}
