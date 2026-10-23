package com.nendo.argosy.data.emulator

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HostSavePathsTest {
    private val realHome = System.getProperty("user.home")
    private lateinit var home: File

    @BeforeTest
    fun useTempHome() {
        home = Files.createTempDirectory("argosy-home").toFile()
        System.setProperty("user.home", home.path)
    }

    @AfterTest
    fun restoreHome() {
        System.setProperty("user.home", realHome)
        home.deleteRecursively()
    }

    @Test
    fun emuDeckThenFlatpakThenNativeWhenNothingExists() {
        assertEquals(
            listOf(
                "${home.path}/Emulation/saves/dolphin/GC",
                "${home.path}/.var/app/org.DolphinEmu.dolphin-emu/data/dolphin-emu/GC",
                "${home.path}/.local/share/dolphin-emu/GC",
                "${home.path}/.dolphin-emu/GC"
            ),
            HostSavePaths.saveDirectories("dolphin", emptyList())
        )
    }

    @Test
    fun anExistingFolderComesFirst() {
        val native = File(home, ".config/PCSX2/memcards").apply { mkdirs() }
        assertEquals(native.path, HostSavePaths.saveDirectories("pcsx2", emptyList()).first())
    }

    @Test
    fun unmappedConfigsDropPhoneStorageFolders() {
        val drastic = SavePathRegistry.getConfig("drastic")!!
        assertTrue(SavePathRegistry.resolvePathWithPackage(drastic, null).isEmpty())
        val phoneFolders = listOf("{extStorage}/Android/data/org.scummvm.scummvm/files/saves", "/data/data/com.retroarch/saves/{core}")
        assertTrue(HostSavePaths.saveDirectories("scummvm", phoneFolders).isEmpty())
    }

    @Test
    fun appPrivateTemplatesStay() {
        assertEquals(
            listOf("{filesDir}/states", "{builtinSaves}"),
            HostSavePaths.saveDirectories("unmapped", listOf("{filesDir}/states", "{builtinSaves}", "{anyStorage}/PSP/SAVEDATA"))
        )
    }

    @Test
    fun retroArchCoreFoldersFollowTheHostConfig() {
        val cfg = File(home, ".var/app/org.libretro.RetroArch/config/retroarch/retroarch.cfg")
        cfg.parentFile.mkdirs()
        cfg.writeText("savefile_directory = \"~/Emulation/saves/retroarch/saves\"\nsavestate_directory = \"default\"\n")
        val ngc = SavePathRegistry.getConfig("retroarch_ngc")!!
        assertEquals(
            listOf("${home.path}/Emulation/saves/retroarch/saves/dolphin-emu/User/GC"),
            SavePathRegistry.resolvePathWithPackage(ngc, null)
        )
        assertEquals(
            listOf("${cfg.parent}/states/{core}", "${cfg.parent}/states"),
            HostSavePaths.stateDirectories("retroarch", StatePathRegistry.getConfig("retroarch")!!.defaultPaths)
        )
    }

    @Test
    fun registryResolvesLinuxFolders() {
        val ppsspp = SavePathRegistry.getConfig("ppsspp")!!
        assertEquals(
            "${home.path}/Emulation/saves/ppsspp/saves",
            SavePathRegistry.resolvePathWithPackage(ppsspp, "org.ppsspp.ppsspp").first()
        )
        val wii = SavePathRegistry.getConfigForPlatform("dolphin", "wii")!!
        assertTrue(SavePathRegistry.resolvePath(wii, "wii").all { it.endsWith("Wii/title/00010000") })
        val ryujinx = SavePathRegistry.getConfig("ryujinx")!!
        assertTrue(SavePathRegistry.resolvePathWithPackage(ryujinx, null).none { "{package}" in it })
    }

    @Test
    fun registryResolvesLinuxStateFolders() {
        val ppsspp = StatePathRegistry.getConfig("ppsspp")!!
        assertEquals(
            listOf(
                "${home.path}/Emulation/saves/ppsspp/states",
                "${home.path}/.var/app/org.ppsspp.PPSSPP/config/ppsspp/PSP/PPSSPP_STATE",
                "${home.path}/.config/ppsspp/PSP/PPSSPP_STATE"
            ),
            StatePathRegistry.resolvePath(ppsspp, "psp")
        )
        val native = File(home, ".local/share/duckstation/savestates").apply { mkdirs() }
        val duckstation = StatePathRegistry.getConfig("duckstation")!!
        assertEquals(native.path, StatePathRegistry.resolvePath(duckstation, "psx").first())
        assertTrue(HostSavePaths.stateDirectories("dolphin", emptyList()).all { it.endsWith("StateSaves") })
    }

    @Test
    fun unmappedStateConfigsDropPhoneStorageFolders() {
        val melonds = StatePathRegistry.getConfig("melonds")!!
        assertTrue(StatePathRegistry.resolvePath(melonds, "nds").isEmpty())
    }
}
