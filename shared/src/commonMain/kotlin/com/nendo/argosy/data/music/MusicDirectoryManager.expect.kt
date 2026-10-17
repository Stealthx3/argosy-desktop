package com.nendo.argosy.data.music

import java.io.File

expect class MusicDirectoryManager {
    companion object {}

    suspend fun resolveMusicDir(): File

    fun defaultMusicDir(): File

    suspend fun targetFileFor(platformName: String, gameName: String, trackNumber: Int?, title: String?, fileName: String): File

    fun targetFileIn(musicDir: File, platformName: String, gameName: String, trackNumber: Int?, title: String?, fileName: String): File

    fun moveIntoMusic(source: File, target: File): Boolean

    suspend fun countFiles(): Int

    suspend fun relocate(source: File, destination: File)

    fun scanFile(file: File)
}
