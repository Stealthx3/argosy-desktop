package com.nendo.argosy.data.music

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class MusicDirectoryManager @Inject constructor() {
    actual companion object {}

    actual suspend fun resolveMusicDir(): File = TODO("desktop: resolveMusicDir")

    actual fun defaultMusicDir(): File = TODO("desktop: defaultMusicDir")

    actual suspend fun targetFileFor(platformName: String, gameName: String, trackNumber: Int?, title: String?, fileName: String): File = TODO("desktop: targetFileFor")

    actual fun targetFileIn(musicDir: File, platformName: String, gameName: String, trackNumber: Int?, title: String?, fileName: String): File = TODO("desktop: targetFileIn")

    actual fun moveIntoMusic(source: File, target: File): Boolean = false

    actual suspend fun countFiles(): Int = 0

    actual suspend fun relocate(source: File, destination: File) {
    }

    actual fun scanFile(file: File) {
    }
}
