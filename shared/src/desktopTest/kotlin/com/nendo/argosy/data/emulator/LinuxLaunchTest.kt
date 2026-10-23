package com.nendo.argosy.data.emulator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinuxLaunchTest {
    private val home = "/home/deck"
    private val retroArchFlatpak = LinuxInstall(LinuxInstallKind.FLATPAK, "org.libretro.RetroArch", "1.22.2")
    private val retroArchNative = LinuxInstall(LinuxInstallKind.NATIVE, "/usr/bin/retroarch", null)

    @Test
    fun retroArchFlatpakRunsTheCoreOnTheRom() {
        assertEquals(
            listOf("flatpak", "run", "org.libretro.RetroArch", "-L", "/cores/gambatte_libretro.so", "/roms/a.gbc"),
            LinuxLaunch.retroArch(retroArchFlatpak, "/cores/gambatte_libretro.so", "/roms/a.gbc")
        )
    }

    @Test
    fun configuredCoreDirectoryComesFirst() {
        val config = "video_driver = \"vulkan\"\nlibretro_directory = \"~/.var/app/org.libretro.RetroArch/config/retroarch/cores/\"\n"
        assertEquals(
            listOf("$home/.var/app/org.libretro.RetroArch/config/retroarch/cores/gambatte_libretro.so"),
            LinuxLaunch.retroArchCoreCandidates(retroArchFlatpak, home, "gambatte", config)
        )
    }

    @Test
    fun nativeRetroArchFallsBackToDistributionFolders() {
        val candidates = LinuxLaunch.retroArchCoreCandidates(retroArchNative, home, "mgba", "libretro_directory = \"default\"")
        assertEquals("$home/.config/retroarch/cores/mgba_libretro.so", candidates.first())
        assertEquals(true, "/usr/lib/libretro/mgba_libretro.so" in candidates)
        assertEquals("$home/.config/retroarch/retroarch.cfg", LinuxLaunch.retroArchConfigPath(retroArchNative, home))
    }

    @Test
    fun standaloneCommandsFollowEachEmulatorsHelp() {
        val dolphin = LinuxInstall(LinuxInstallKind.FLATPAK, "org.DolphinEmu.dolphin-emu", null)
        assertEquals(
            listOf("flatpak", "run", "org.DolphinEmu.dolphin-emu", "-b", "-e", "/roms/g.rvz"),
            LinuxLaunch.standalone("dolphin", dolphin, "/roms/g.rvz")
        )
        val cemu = LinuxInstall(LinuxInstallKind.APPIMAGE, "$home/Applications/Cemu.AppImage", null)
        assertEquals(listOf("$home/Applications/Cemu.AppImage", "-g", "/roms/w.wua"), LinuxLaunch.standalone("cemu", cemu, "/roms/w.wua"))
    }

    @Test
    fun unverifiedEmulatorsHaveNoCommand() {
        val staging = LinuxInstall(LinuxInstallKind.FLATPAK, "io.github.dosbox-staging", null)
        assertNull(LinuxLaunch.standalone("dosbox_turbo", staging, "/roms/d.zip"))
        val duck = LinuxInstall(LinuxInstallKind.APPIMAGE, "$home/Applications/DuckStation.AppImage", null)
        assertNull(LinuxLaunch.standalone("duckstation", duck, "/roms/p.chd"))
    }

    @Test
    fun intentCarriesTheCommand() {
        val command = listOf("retroarch", "-L", "core.so", "rom.gb")
        assertEquals(command, LinuxLaunch.commandOf(LinuxLaunch.intent("com.retroarch", command)))
    }
}
