package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.model.CatalogPackage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmulatorCatalogRulesTest {
    private val home = "/home/deck"
    private val dolphin = LinuxEmulator(
        id = "dolphin",
        displayName = "Dolphin",
        flatpakIds = listOf("org.DolphinEmu.dolphin-emu"),
        appImagePrefixes = listOf("Dolphin"),
        flathubId = "org.DolphinEmu.dolphin-emu"
    )
    private val retroarch = LinuxEmulator(id = "retroarch", displayName = "RetroArch", flathubId = "org.libretro.RetroArch", appImageRepo = "libretro/RetroArch")
    private val flatpak = LinuxInstall(LinuxInstallKind.FLATPAK, "org.DolphinEmu.dolphin-emu", "2603")

    @Test
    fun emuDeckManagedNeedsAnInstallAndALauncherThatNamesIt() {
        val launcher = "#!/bin/bash\n/usr/bin/flatpak run org.DolphinEmu.dolphin-emu \"\$@\"\n"
        assertTrue(EmulatorCatalogRules.emuDeckManaged(dolphin, flatpak, launcher))
        assertFalse(EmulatorCatalogRules.emuDeckManaged(dolphin, null, launcher))
        assertFalse(EmulatorCatalogRules.emuDeckManaged(dolphin, flatpak, "flatpak run org.ppsspp.PPSSPP"))
    }

    @Test
    fun installOptionsFollowTheKnownSources() {
        assertEquals(listOf(CatalogPackage.FLATPAK), EmulatorCatalogRules.installOptions(dolphin))
        assertEquals(listOf(CatalogPackage.FLATPAK, CatalogPackage.APPIMAGE), EmulatorCatalogRules.installOptions(retroarch))
    }

    @Test
    fun uninstallRemovesFlatpaksAndAppImagesButNeverSystemPackages() {
        assertEquals(listOf("flatpak", "uninstall", "-y", "--noninteractive", "org.DolphinEmu.dolphin-emu"), EmulatorCatalogRules.uninstallCommand(flatpak))
        assertEquals(listOf("rm", "-f", "/home/deck/Applications/Cemu.AppImage"), EmulatorCatalogRules.uninstallCommand(LinuxInstall(LinuxInstallKind.APPIMAGE, "/home/deck/Applications/Cemu.AppImage", null)))
        assertNull(EmulatorCatalogRules.uninstallCommand(LinuxInstall(LinuxInstallKind.NATIVE, "/usr/bin/dolphin-emu", null)))
    }

    @Test
    fun biosFolderIsEmuDeckSharedRetroArchSystemOrUnknown() {
        assertEquals("$home/Emulation/bios", EmulatorCatalogRules.biosFolder(dolphin, flatpak, home, emuDeckBios = true, retroArchSystemDir = null))
        assertNull(EmulatorCatalogRules.biosFolder(dolphin, flatpak, home, emuDeckBios = false, retroArchSystemDir = null))
        assertNull(EmulatorCatalogRules.biosFolder(dolphin, null, home, emuDeckBios = true, retroArchSystemDir = null))
        val ra = LinuxInstall(LinuxInstallKind.FLATPAK, "org.libretro.RetroArch", null)
        assertEquals("$home/.var/app/org.libretro.RetroArch/config/retroarch/system", EmulatorCatalogRules.biosFolder(retroarch, ra, home, false, null))
        assertEquals("/data/system", EmulatorCatalogRules.biosFolder(retroarch, ra, home, false, "/data/system"))
    }

    @Test
    fun retroArchSystemDirectoryReadsTheConfigLine() {
        assertEquals("$home/bios", EmulatorCatalogRules.retroArchSystemDirectory("system_directory = \"~/bios\"\n", home))
        assertNull(EmulatorCatalogRules.retroArchSystemDirectory("system_directory = \"default\"\n", home))
        assertNull(EmulatorCatalogRules.retroArchSystemDirectory("system_directory_alt = \"/x\"\n", home))
    }

    @Test
    fun dataFolderIsTheFlatpakAppFolder() {
        assertEquals("$home/.var/app/org.DolphinEmu.dolphin-emu", EmulatorCatalogRules.dataFolder(flatpak, home))
        assertNull(EmulatorCatalogRules.dataFolder(LinuxInstall(LinuxInstallKind.NATIVE, "/usr/bin/x", null), home))
    }
}
