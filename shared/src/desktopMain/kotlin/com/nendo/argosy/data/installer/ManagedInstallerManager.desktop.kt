package com.nendo.argosy.data.installer

import com.nendo.argosy.data.local.entity.ManagedInstallerEntity
import com.nendo.argosy.data.emulator.EmulatorDetector
import com.nendo.argosy.data.emulator.EmulatorRegistry
import com.nendo.argosy.data.emulator.LinuxEmulators
import com.nendo.argosy.data.emulator.LinuxInstallKind
import com.nendo.argosy.data.remote.github.GitHubAsset
import com.nendo.argosy.data.remote.github.GitHubReleaseClient
import com.nendo.argosy.data.remote.github.ReleaseLookup
import com.nendo.argosy.platform.DesktopDirectories
import com.nendo.argosy.platform.HostShell
import com.nendo.argosy.util.SafeCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Desktop managed installers: GitHub rows install the release's x86-64 AppImage into
 * ~/Applications (replacing the previous file on update); rows under [FLATHUB_OWNER] are the
 * emulators installed as Flatpaks, kept in sync at each refresh and updated with `flatpak update`.
 */
@Singleton
actual class ManagedInstallerManager @Inject constructor(
    private val repository: ManagedInstallerRepository,
    private val releaseClient: GitHubReleaseClient,
    private val emulatorDetector: EmulatorDetector,
) {
    private val scope = SafeCoroutineScope(Dispatchers.IO, "ManagedInstallerManager")
    private val _job = MutableStateFlow<InstallerJob?>(null)
    actual val job: StateFlow<InstallerJob?> = _job.asStateFlow()

    actual fun isInstalled(packageName: String?): Boolean {
        val (kind, emulatorPackage) = InstallToken.parse(packageName) ?: return false
        return emulatorDetector.linuxInstall(emulatorPackage)?.kind == kind
    }

    actual fun canInstallPackages(): Boolean = true

    actual fun openInstallPermissionSettings() = Unit

    actual fun openImeSettings() = Unit

    actual fun showImePicker() = Unit

    actual fun isImeEnabled(packageName: String): Boolean = false

    actual fun isImeActive(packageName: String): Boolean = false

    actual fun clearJob() {
        _job.value = null
    }

    actual fun reconcilePendingInstall() = Unit

    actual suspend fun refresh(rows: List<ManagedInstallerEntity>) {
        emulatorDetector.detectEmulators()
        repository.syncPlatformRows(FLATHUB_OWNER, installedFlatpakRows())
        repository.getAll().forEach { row ->
            if (row.repoOwner == FLATHUB_OWNER) {
                val installed = flatpakVersion(listOf("flatpak", "info", "--user", row.repoName)) ?: return@forEach
                repository.recordInstall(row.id, installed, row.packageName, null)
                repository.recordCheck(row.id, flatpakVersion(listOf("flatpak", "remote-info", "--user", "flathub", row.repoName)) ?: installed)
            } else {
                when (val lookup = releaseClient.latestRelease(row.repoOwner, row.repoName)) {
                    is ReleaseLookup.Found -> repository.recordCheck(row.id, lookup.release.tagName)
                    ReleaseLookup.NotFound -> repository.recordCheck(row.id, null)
                    is ReleaseLookup.RateLimited -> return
                    is ReleaseLookup.Failed -> Unit
                }
            }
        }
    }

    actual fun install(row: ManagedInstallerEntity, chosenAsset: GitHubAsset?) {
        val current = _job.value
        if (current != null && !current.state.isDone() && !(chosenAsset != null && current.installerId == row.id)) return
        _job.value = InstallerJob(row.id, InstallerJobState.Checking)
        scope.launch {
            if (row.repoOwner == FLATHUB_OWNER) updateFlatpak(row) else installAppImage(row, chosenAsset)
        }
    }

    private suspend fun updateFlatpak(row: ManagedInstallerEntity) {
        _job.value = InstallerJob(row.id, InstallerJobState.Downloading(0f))
        HostShell.run(listOf("flatpak", "update", "--user", "-y", "--noninteractive", row.repoName))
        val version = flatpakVersion(listOf("flatpak", "info", "--user", row.repoName))
            ?: return fail(row.id, InstallerFailure.DOWNLOAD)
        repository.recordInstall(row.id, version, row.packageName, null)
        _job.value = null
    }

    private suspend fun installAppImage(row: ManagedInstallerEntity, chosenAsset: GitHubAsset?) {
        val release = when (val lookup = releaseClient.latestRelease(row.repoOwner, row.repoName)) {
            is ReleaseLookup.Found -> lookup.release
            ReleaseLookup.NotFound -> return fail(row.id, InstallerFailure.NOT_FOUND)
            is ReleaseLookup.RateLimited -> return fail(row.id, InstallerFailure.RATE_LIMITED)
            is ReleaseLookup.Failed -> return fail(row.id, InstallerFailure.NO_RELEASE)
        }
        val asset = chosenAsset ?: run {
            val candidates = AppImageAssets.candidates(release.assets)
            when (candidates.size) {
                0 -> return fail(row.id, InstallerFailure.NO_APK)
                1 -> candidates.single()
                else -> {
                    _job.value = InstallerJob(row.id, InstallerJobState.NeedsVariantChoice(candidates))
                    return
                }
            }
        }
        val downloaded = download(row.id, asset) ?: return fail(row.id, InstallerFailure.DOWNLOAD)
        val home = System.getProperty("user.home")
        val target = "$home/Applications/${asset.name}"
        HostShell.run(listOf("mkdir", "-p", "$home/Applications"))
        HostShell.run(listOf("install", "-m", "755", downloaded.absolutePath, target))
        downloaded.delete()
        row.assetVariant?.takeIf { it != asset.name }?.let { HostShell.run(listOf("rm", "-f", "$home/Applications/$it")) }
        emulatorDetector.detectEmulators()
        if (!isInstalled(row.packageName)) return fail(row.id, InstallerFailure.DOWNLOAD)
        repository.recordInstall(row.id, release.tagName, row.packageName, asset.name)
        _job.value = null
    }

    private fun download(installerId: Long, asset: GitHubAsset): File? = runCatching {
        val dir = DesktopDirectories.cache.resolve("installers").apply { mkdirs() }
        val file = File(dir, asset.name)
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        val response = client.send(HttpRequest.newBuilder(URI(asset.downloadUrl)).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) return null
        val total = asset.size.coerceAtLeast(1)
        var read = 0L
        response.body().use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    read += count
                    _job.value = InstallerJob(installerId, InstallerJobState.Downloading(read.toFloat() / total))
                }
            }
        }
        file
    }.getOrNull()

    private fun installedFlatpakRows(): List<ManagedInstallerEntity> =
        LinuxEmulators.all.mapNotNull { entry ->
            val flathubId = entry.flathubId ?: return@mapNotNull null
            val def = EmulatorRegistry.getById(entry.id) ?: return@mapNotNull null
            val install = emulatorDetector.linuxInstall(def.packageName) ?: return@mapNotNull null
            if (install.kind != LinuxInstallKind.FLATPAK || install.target != flathubId) return@mapNotNull null
            ManagedInstallerEntity(
                repoOwner = FLATHUB_OWNER,
                repoName = flathubId,
                displayName = entry.displayName,
                packageName = InstallToken.of(LinuxInstallKind.FLATPAK, def.packageName),
                locked = true,
                sortOrder = FLATHUB_SORT_BASE,
            )
        }.distinctBy { it.repoName }

    private fun flatpakVersion(args: List<String>): String? =
        HostShell.run(args)?.lineSequence()?.map { it.trim() }
            ?.firstOrNull { it.startsWith("Version:") }?.substringAfter(':')?.trim()?.takeIf { it.isNotEmpty() }

    private fun fail(installerId: Long, reason: InstallerFailure) {
        _job.value = InstallerJob(installerId, InstallerJobState.Failed(reason))
    }

    private fun InstallerJobState.isDone(): Boolean =
        this is InstallerJobState.Idle || this is InstallerJobState.Failed

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val FLATHUB_SORT_BASE = 100
    }
}

object AppImageAssets {
    private val OTHER_ARCH = listOf("arm64", "armhf", "aarch64", "-dbg")

    fun candidates(assets: List<GitHubAsset>): List<GitHubAsset> =
        assets.filter { asset ->
            asset.name.endsWith(".appimage", ignoreCase = true) &&
                OTHER_ARCH.none { asset.name.contains(it, ignoreCase = true) }
        }
}

/**
 * The installer row's package token on the desktop: the install kind it manages and the upstream
 * emulator package, so an AppImage row is not marked installed by the same emulator's Flatpak.
 */
object InstallToken {
    fun of(kind: LinuxInstallKind, emulatorPackage: String): String = "${kind.name.lowercase()}:$emulatorPackage"

    fun parse(token: String?): Pair<LinuxInstallKind, String>? {
        val kind = token?.substringBefore(':', "")?.let { name -> LinuxInstallKind.entries.find { it.name.lowercase() == name } }
            ?: return null
        return kind to token.substringAfter(':')
    }
}
