package com.nendo.argosy.data.steam

import java.io.File

expect class SteamPathResolver {
    suspend fun getConfiguredTarget(): SteamInstallTarget

    fun resolveBaseForTarget(target: SteamInstallTarget): String?

    suspend fun getInstallDirByName(dirName: String): File

    suspend fun snapshotInstallDirAtEnqueue(appId: Long, steamInstallDir: String?): File

    suspend fun getInstallDir(appId: Long): File

    suspend fun getSteamDir(): File

    suspend fun getResolvedSteamBase(): String?

    suspend fun isGameInstalled(appId: Long): Boolean

    suspend fun isGameInstalled(game: com.nendo.argosy.data.local.entity.GameEntity): Boolean

    fun findGnStoragePath(): String?

    fun findAllGnStoragePaths(): List<String>

    fun sanitizeGameName(name: String): String

    fun getAvailableBytes(dir: File): Long?

    fun getDirectorySize(dir: File): Long

    fun moveDirectory(source: File, destination: File): Boolean

    fun getAvailableVolumes(): List<SteamInstallVolume>
}
