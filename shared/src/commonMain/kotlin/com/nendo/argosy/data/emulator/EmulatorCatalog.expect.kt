package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.model.CatalogEmulator

/**
 * The emulators this platform can install and manage: every compatible emulator with its install
 * state, plus install, uninstall, folder and BIOS actions. Unsupported where [isSupported] is false.
 */
expect class EmulatorCatalog {
    val isSupported: Boolean

    suspend fun entries(): List<CatalogEmulator>

    suspend fun installFlatpak(id: String): Boolean

    suspend fun uninstall(id: String): Boolean

    fun openFolder(path: String)

    suspend fun copyBios(sourcePath: String, folder: String): Boolean
}
