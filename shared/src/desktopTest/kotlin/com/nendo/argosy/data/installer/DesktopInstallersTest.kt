package com.nendo.argosy.data.installer

import com.nendo.argosy.data.local.entity.ManagedInstallerEntity
import com.nendo.argosy.data.remote.github.GitHubAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopInstallersTest {
    private fun asset(name: String) = GitHubAsset(name, "https://example.invalid/$name", 1)

    @Test
    fun onlyX64AppImagesAreCandidates() {
        val xemu = listOf(
            "xemu-0.8.139-aarch64.AppImage", "xemu-0.8.139-dbg-aarch64.AppImage",
            "xemu-0.8.139-dbg-x86_64.AppImage", "xemu-0.8.139-x86_64.AppImage", "xemu-win-x86_64.zip"
        ).map(::asset)
        assertEquals(listOf("xemu-0.8.139-x86_64.AppImage"), AppImageAssets.candidates(xemu).map { it.name })
        val mgba = listOf("mGBA-0.10.5-appimage-arm64.appimage", "mGBA-0.10.5-appimage-x64.appimage").map(::asset)
        assertEquals(listOf("mGBA-0.10.5-appimage-x64.appimage"), AppImageAssets.candidates(mgba).map { it.name })
        val duck = listOf("DuckStation-arm64.AppImage", "DuckStation-armhf.AppImage", "DuckStation-x64-SSE2.AppImage", "DuckStation-x64.AppImage").map(::asset)
        assertEquals(2, AppImageAssets.candidates(duck).size)
    }

    @Test
    fun desktopSeedsAreTheVerifiedAppImageProjects() {
        val seeds = InstallerSeeds.all
        assertEquals(10, seeds.size)
        assertTrue(InstallerSeeds.replacesUpstreamSeeds)
        assertTrue(seeds.all { it.locked && it.packageName != null })
        assertTrue(seeds.none { it.repoName == "HandheldKeyboard" || it.repoName == "GameNative" || it.repoName == "DroidDeck" })
        assertTrue(seeds.any { it.repoOwner == "RPCS3" && it.repoName == "rpcs3-binaries-linux" })
    }

    @Test
    fun installTokensCarryTheInstallKind() {
        val token = InstallToken.of(com.nendo.argosy.data.emulator.LinuxInstallKind.APPIMAGE, "org.mupen64plusae.v3.fzurita")
        assertEquals("appimage:org.mupen64plusae.v3.fzurita", token)
        assertEquals(com.nendo.argosy.data.emulator.LinuxInstallKind.APPIMAGE to "org.mupen64plusae.v3.fzurita", InstallToken.parse(token))
        assertEquals(null, InstallToken.parse("com.example.plain"))
        assertTrue(InstallerSeeds.all.all { it.packageName!!.startsWith("appimage:") })
    }
}
