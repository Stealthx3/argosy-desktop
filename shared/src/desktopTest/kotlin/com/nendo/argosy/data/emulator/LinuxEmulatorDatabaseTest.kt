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
    fun everyEntryIsAnUpstreamIdOrListsItsOwnPlatforms() {
        assertEquals(41, bundled.emulators.size)
        val upstream = bundled.emulators.filter { it.platforms.isEmpty() }
        assertEquals(20, upstream.size)
        upstream.forEach { assertNotNull(EmulatorRegistry.getById(it.id), it.id) }
        bundled.emulators.filter { it.platforms.isNotEmpty() }.forEach { linux ->
            assertTrue(linux.id.startsWith("linux_"), linux.id)
            assertNotNull(linux.launch, linux.id)
        }
    }

    @Test
    fun desktopOnlyEntriesJoinTheRegistryForTheirPlatforms() {
        EmulatorRegistry.registerPlatformEmulators(LinuxEmulators.desktopOnlyDefs(bundled.emulators))
        val stella = EmulatorRegistry.getById("linux_stella")
        assertNotNull(stella)
        assertEquals(setOf("atari2600"), stella.supportedPlatforms)
        assertTrue(EmulatorRegistry.getForPlatform("atari2600").any { it.id == "linux_stella" })
        assertTrue(EmulatorRegistry.isKnownPackage(stella.packageName))
        assertEquals("pcsx2", EmulatorRegistry.getById("pcsx2")?.id)
    }

    @Test
    fun desktopOnlyLaunchArgumentsFollowEachEmulatorsCommandLine() {
        val flatpak = { id: String -> LinuxInstall(LinuxInstallKind.FLATPAK, id, null) }
        assertEquals(
            listOf("flatpak", "run", "org.purei.Play", "--disc", "/g/p.iso"),
            LinuxLaunch.standalone("linux_play", flatpak("org.purei.Play"), "/g/p.iso")
        )
        assertEquals(
            listOf("flatpak", "run", "net.shadps4.shadPS4", "-d", "-g", "/g/e.bin"),
            LinuxLaunch.standalone("linux_shadps4", flatpak("net.shadps4.shadPS4"), "/g/e.bin")
        )
        assertEquals(
            null,
            LinuxLaunch.standalone("linux_shadps4", LinuxInstall(LinuxInstallKind.NATIVE, "/usr/bin/shadps4", null), "/g/e.bin")
        )
        assertEquals(
            listOf("flatpak", "run", "net.fsuae.FS-UAE", "--floppy_drive_0=/g/a.adf"),
            LinuxLaunch.standalone("linux_fsuae", flatpak("net.fsuae.FS-UAE"), "/g/a.adf")
        )
        assertEquals(listOf("/usr/bin/mednafen", "/g/s.cue"), LinuxLaunch.standalone("linux_mednafen", LinuxInstall(LinuxInstallKind.NATIVE, "/usr/bin/mednafen", null), "/g/s.cue"))
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
