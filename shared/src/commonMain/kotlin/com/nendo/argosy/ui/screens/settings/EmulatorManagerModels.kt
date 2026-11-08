package com.nendo.argosy.ui.screens.settings

import com.nendo.argosy.data.model.CatalogEmulator
import com.nendo.argosy.data.model.CatalogPackage

enum class EmulatorPickerKind { INSTALL_SOURCE, BIOS_SOURCE, FIRMWARE }

data class EmulatorPickerOption(val token: String, val label: String? = null, val labelRes: Int? = null)

data class EmulatorPicker(
    val kind: EmulatorPickerKind,
    val options: List<EmulatorPickerOption>,
    val focusIndex: Int = 0
)

data class EmulatorManagerState(
    val isSupported: Boolean = false,
    val loading: Boolean = false,
    val emulators: List<CatalogEmulator> = emptyList(),
    val selectedId: String? = null,
    val busy: Boolean = false,
    val messageRes: Int? = null,
    val picker: EmulatorPicker? = null,
    val confirmUninstall: Boolean = false
) {
    val selected: CatalogEmulator? get() = emulators.firstOrNull { it.id == selectedId }
}

const val PICKER_TOKEN_FLATPAK = "flatpak"
const val PICKER_TOKEN_APPIMAGE = "appimage"
const val PICKER_TOKEN_ROMM = "romm"
const val PICKER_TOKEN_FILE = "file"

fun CatalogPackage.pickerToken(): String = when (this) {
    CatalogPackage.FLATPAK -> PICKER_TOKEN_FLATPAK
    CatalogPackage.APPIMAGE -> PICKER_TOKEN_APPIMAGE
    CatalogPackage.NATIVE -> name.lowercase()
}
