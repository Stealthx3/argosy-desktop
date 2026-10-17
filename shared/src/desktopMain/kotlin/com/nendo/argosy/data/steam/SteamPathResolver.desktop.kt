package com.nendo.argosy.data.steam

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class SteamPathResolver @Inject constructor() {
    actual suspend fun getConfiguredTarget(): SteamInstallTarget = TODO("desktop: getConfiguredTarget")

    actual fun resolveBaseForTarget(target: SteamInstallTarget): String? = null

    actual suspend fun getInstallDirByName(dirName: String): File = TODO("desktop: getInstallDirByName")

    actual suspend fun snapshotInstallDirAtEnqueue(appId: Long, steamInstallDir: String?): File = TODO("desktop: snapshotInstallDirAtEnqueue")

    actual suspend fun getInstallDir(appId: Long): File = TODO("desktop: getInstallDir")

    actual suspend fun getSteamDir(): File = TODO("desktop: getSteamDir")

    actual suspend fun getResolvedSteamBase(): String? = null

    actual suspend fun isGameInstalled(appId: Long): Boolean = false

    actual suspend fun isGameInstalled(game: com.nendo.argosy.data.local.entity.GameEntity): Boolean = false

    actual fun findGnStoragePath(): String? = null

    actual fun findAllGnStoragePaths(): List<String> = emptyList()

    actual fun sanitizeGameName(name: String): String = ""

    actual fun getAvailableBytes(dir: File): Long? = null

    actual fun getDirectorySize(dir: File): Long = 0L

    actual fun moveDirectory(source: File, destination: File): Boolean = false

    actual fun getAvailableVolumes(): List<SteamInstallVolume> = emptyList()
}
