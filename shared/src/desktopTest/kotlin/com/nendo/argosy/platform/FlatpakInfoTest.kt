package com.nendo.argosy.platform

import com.nendo.argosy.data.model.SandboxPermission
import com.nendo.argosy.ui.common.FlatpakOverrideCommand
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlatpakInfoTest {
    private val home = "/home/tester"

    private fun info(filesystems: String, extraContext: String = "", bus: String = "org.freedesktop.Flatpak=talk") = """
        [Application]
        name=io.github.stealthx3.icebreaker-client

        [Context]
        shared=network;ipc;
        sockets=x11;wayland;pulseaudio;
        devices=dri;all;
        filesystems=$filesystems
        $extraContext

        [Session Bus Policy]
        $bus
    """.trimIndent()

    private val fullFilesystems = (
        listOf("~/Games/IceBreaker:create") + FlatpakInfo.STEAM_FOLDERS.map { "$it:rw" } + FlatpakInfo.EMULATOR_FOLDERS
    ).joinToString(";") + ";"

    @Test
    fun theShippedManifestGrantsEverything() {
        val report = FlatpakInfo.report(info(fullFilesystems), home)
        assertTrue(report.sandboxed)
        assertTrue(report.granted.values.all { it }, "missing: ${report.missingFlags}")
    }

    @Test
    fun aRevokedFolderAndBusNameShowAsMissingWithTheirFlags() {
        val filesystems = fullFilesystems.replace("~/.var/app/net.pcsx2.PCSX2;", "")
        val report = FlatpakInfo.report(info(filesystems, bus = ""), home)
        assertFalse(report.granted.getValue(SandboxPermission.EMULATOR_FOLDERS))
        assertEquals(listOf("--filesystem=~/.var/app/net.pcsx2.PCSX2"), report.missingFlags[SandboxPermission.EMULATOR_FOLDERS])
        assertFalse(report.granted.getValue(SandboxPermission.HOST_ACCESS))
        assertTrue(report.granted.getValue(SandboxPermission.GAMES_FOLDER))
    }

    @Test
    fun homeAccessCoversTheGamesFolder() {
        val report = FlatpakInfo.report(info("home;"), home)
        assertTrue(report.granted.getValue(SandboxPermission.GAMES_FOLDER))
        assertFalse(report.granted.getValue(SandboxPermission.EMULATOR_FOLDERS))
    }

    @Test
    fun aMissingSteamFolderShowsItsOwnFix() {
        val filesystems = fullFilesystems.replace("~/.var/app/com.valvesoftware.Steam:rw;", "")
        val report = FlatpakInfo.report(info(filesystems), home)
        assertFalse(report.granted.getValue(SandboxPermission.STEAM_FOLDERS))
        assertEquals(listOf("--filesystem=~/.var/app/com.valvesoftware.Steam:rw"), report.missingFlags[SandboxPermission.STEAM_FOLDERS])
    }

    @Test
    fun portalsAreAlwaysAvailable() {
        val report = FlatpakInfo.report(info(""), home)
        assertTrue(report.granted.getValue(SandboxPermission.NOTIFICATIONS))
        assertTrue(report.granted.getValue(SandboxPermission.SCREENSHOTS))
    }

    @Test
    fun theFixCommandNamesTheApp() {
        assertEquals(
            "flatpak override --user --talk-name=org.freedesktop.Flatpak io.github.stealthx3.icebreaker-client",
            FlatpakOverrideCommand.of(listOf(SandboxPermission.HOST_ACCESS.flag))
        )
    }

    @Test
    fun theEmulatorFolderListMatchesTheManifest() {
        val manifest = File("../desktopApp/flatpak/io.github.stealthx3.icebreaker-client.yml").readLines()
            .map { it.trim() }
            .filter { it.startsWith("- --filesystem=") }
            .map { it.removePrefix("- --filesystem=") }
            .filterNot { it.startsWith("~/Games/IceBreaker") }
        val steam = FlatpakInfo.STEAM_FOLDERS.map { "$it:rw" }
        assertTrue(manifest.containsAll(steam))
        assertEquals(manifest - FlatpakInfo.REMOVABLE_MEDIA_FOLDERS.toSet() - steam.toSet(), FlatpakInfo.EMULATOR_FOLDERS)
        assertTrue(manifest.containsAll(FlatpakInfo.REMOVABLE_MEDIA_FOLDERS))
    }

    @Test
    fun theHostViewOfAGrantedOverrideReadsAsGranted() {
        val hostView = info("~/Games/IceBreaker:create;", bus = "org.freedesktop.Flatpak=talk")
        assertTrue(FlatpakInfo.report(hostView, home).granted.getValue(SandboxPermission.HOST_ACCESS))
        assertEquals(
            listOf("flatpak", "override", "--user", "--device=all", SandboxPermission.APP_ID),
            SandboxPermission.overrideArgs(listOf("--device=all"))
        )
    }
}
