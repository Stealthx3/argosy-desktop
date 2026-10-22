package com.nendo.argosy.data.emulator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LinuxEmulatorsTest {
    private val output = """
        /home/deck
        @@FLATPAK
        org.libretro.RetroArch	1.22.2
        org.DolphinEmu.dolphin-emu	2609a
        nl.jknaapen.fladder
        @@PATH
        dosbox-x	/usr/bin/dosbox-x
        @@APPS
        /home/deck/Applications/Cemu.AppImage
        /home/deck/Applications/DuckStation.AppImage.backup
        /home/deck/Applications/rpcs3.AppImage_old
        /home/deck/Applications/Vita3K/Vita3K
        /home/deck/Applications/publish/Ryujinx
    """.trimIndent()

    private val parsed = LinuxEmulators.parseInventory(output)
    private val home = parsed.first
    private val inventory = parsed.second

    private fun install(id: String) =
        LinuxEmulators.findInstall(LinuxEmulators.all.first { it.id == id }, inventory, "$home/Applications")

    @Test
    fun parsesAllSections() {
        assertEquals("/home/deck", home)
        assertEquals("1.22.2", inventory.flatpaks["org.libretro.RetroArch"])
        assertNull(inventory.flatpaks["nl.jknaapen.fladder"])
        assertEquals(true, "nl.jknaapen.fladder" in inventory.flatpaks)
        assertEquals("/usr/bin/dosbox-x", inventory.pathBinaries["dosbox-x"])
        assertEquals(5, inventory.applications.size)
    }

    @Test
    fun flatpakInstallCarriesVersion() {
        assertEquals(LinuxInstall(LinuxInstallKind.FLATPAK, "org.libretro.RetroArch", "1.22.2"), install("retroarch"))
    }

    @Test
    fun appImagesMatchByPrefixAndSuffixOnly() {
        assertEquals(LinuxInstallKind.APPIMAGE, install("cemu")?.kind)
        assertNull(install("duckstation"))
        assertNull(install("aps3e"))
    }

    @Test
    fun emuDeckFolderInstallsAndPathBinaries() {
        assertEquals("/home/deck/Applications/Vita3K/Vita3K", install("vita3k")?.target)
        assertEquals("/home/deck/Applications/publish/Ryujinx", install("ryujinx")?.target)
        assertEquals(LinuxInstall(LinuxInstallKind.NATIVE, "/usr/bin/dosbox-x", null), install("dosbox_turbo"))
    }

    @Test
    fun everyLinuxEntryNamesAnUpstreamEmulator() {
        for (linux in LinuxEmulators.all) {
            assertNotNull(EmulatorRegistry.getById(linux.id), linux.id)
        }
        assertEquals(LinuxEmulators.all.size, LinuxEmulators.all.map { EmulatorRegistry.getById(it.id)!!.packageName }.toSet().size)
    }
}
