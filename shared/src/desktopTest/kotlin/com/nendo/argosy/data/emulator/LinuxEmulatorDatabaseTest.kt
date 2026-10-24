package com.nendo.argosy.data.emulator

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinuxEmulatorDatabaseTest {
    private val bundled = LinuxEmulatorDatabase.bundled()

    @Test
    fun everyEntryIsAnUpstreamEmulatorId() {
        assertEquals(20, bundled.emulators.size)
        bundled.emulators.forEach { assertNotNull(EmulatorRegistry.getById(it.id), it.id) }
    }

    @Test
    fun theOldTableCarriesOver() {
        val rpcs3 = bundled.emulators.single { it.id == "aps3e" }
        assertEquals("RPCS3", rpcs3.displayName)
        assertEquals(listOf("net.rpcs3.RPCS3"), rpcs3.flatpakIds)
        assertEquals(listOf("publish/Ryujinx"), bundled.emulators.single { it.id == "ryujinx" }.installPaths)
        assertEquals(
            listOf("com.dosbox_x.DOSBox-X", "io.github.dosbox-staging"),
            bundled.emulators.single { it.id == "dosbox_turbo" }.flatpakIds
        )
    }

    @Test
    fun launchArgumentsComeFromTheDatabase() {
        val flatpak = { id: String -> LinuxInstall(LinuxInstallKind.FLATPAK, id, null) }
        assertEquals(
            listOf("flatpak", "run", "org.DolphinEmu.dolphin-emu", "-b", "-e", "/g/a.iso"),
            LinuxLaunch.standalone("dolphin", flatpak("org.DolphinEmu.dolphin-emu"), "/g/a.iso")
        )
        assertEquals(listOf("/usr/bin/cemu", "-g", "/g/b.wua"), LinuxLaunch.standalone("cemu", LinuxInstall(LinuxInstallKind.NATIVE, "/usr/bin/cemu", null), "/g/b.wua"))
        assertEquals(listOf("flatpak", "run", "app.xemu.xemu", "-dvd_path", "/g/x.iso"), LinuxLaunch.standalone("hakux", flatpak("app.xemu.xemu"), "/g/x.iso"))
        assertNotNull(LinuxLaunch.standalone("dosbox_turbo", flatpak("com.dosbox_x.DOSBox-X"), "/g/d.zip"))
        assertNull(LinuxLaunch.standalone("dosbox_turbo", flatpak("io.github.dosbox-staging"), "/g/d.zip"))
        assertNull(LinuxLaunch.standalone("duckstation", flatpak("org.duckstation.DuckStation"), "/g/p.chd"))
    }

    @Test
    fun aNewerFetchedCopyIsCachedAndOlderOrBrokenOnesAreIgnored() {
        val dir = Files.createTempDirectory("emudb").toFile()
        val cache = File(dir, "linux-emulators.json")
        val bundledText = javaClass.getResource("/emulators/linux-emulators.json")!!.readText()
        assertFalse(LinuxEmulatorDatabase.refresh(fetch = { "not json" }, cacheFile = cache))
        assertFalse(LinuxEmulatorDatabase.refresh(fetch = { bundledText }, cacheFile = cache))
        assertFalse(cache.exists())
        val newer = bundledText.replaceFirst("\"version\": ${bundled.version}", "\"version\": ${bundled.version + 1}")
        assertTrue(LinuxEmulatorDatabase.refresh(fetch = { newer }, cacheFile = cache))
        assertEquals(bundled.version + 1, LinuxEmulatorDatabase.load(cache).version)
        cache.writeText("{")
        assertEquals(bundled.version, LinuxEmulatorDatabase.load(cache).version)
        dir.deleteRecursively()
    }
}
