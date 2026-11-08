package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.model.CatalogEmulator
import javax.inject.Inject

actual class EmulatorCatalog @Inject constructor() {
    actual val isSupported: Boolean = false

    actual suspend fun entries(): List<CatalogEmulator> = emptyList()

    actual suspend fun installFlatpak(id: String): Boolean = false

    actual suspend fun uninstall(id: String): Boolean = false

    actual fun openFolder(path: String) = Unit

    actual suspend fun copyBios(sourcePath: String, folder: String): Boolean = false
}
