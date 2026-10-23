package com.nendo.argosy.data.emulator

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            HostSavePaths.saveDirectories("dolphin")
        )
    }

    @Test
    fun anExistingFolderComesFirst() {
        val native = File(home, ".config/PCSX2/memcards").apply { mkdirs() }
        assertEquals(native.path, HostSavePaths.saveDirectories("pcsx2")?.first())
    }

    @Test
    fun unmappedConfigsKeepTheirTemplates() {
        assertNull(HostSavePaths.saveDirectories("drastic"))
        val drastic = SavePathRegistry.getConfig("drastic")!!
        assertTrue(SavePathRegistry.resolvePathWithPackage(drastic, null).all { "/DraStic/" in it || "/Android/data/" in it })
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
}
