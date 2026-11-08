package com.nendo.argosy.data.model

enum class CatalogPackage { FLATPAK, APPIMAGE, NATIVE }

/**
 * One emulator Argosy knows how to run on this system, installed or not. [platforms] are canonical
 * platform slugs; [biosFolder] is null where no BIOS folder is known for the install.
 */
data class CatalogEmulator(
    val id: String,
    val displayName: String,
    val platforms: List<String>,
    val installed: CatalogPackage?,
    val version: String?,
    val emuDeckManaged: Boolean,
    val installOptions: List<CatalogPackage>,
    val dataFolder: String?,
    val biosFolder: String?,
    val logFolder: String,
    val appImageRepo: String?
)
