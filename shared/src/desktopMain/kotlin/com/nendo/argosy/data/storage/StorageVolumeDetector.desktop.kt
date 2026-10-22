package com.nendo.argosy.data.storage

import com.nendo.argosy.core.storage.StorageVolume as AppStorageVolume
import com.nendo.argosy.core.storage.StorageVolumeType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val REMOVABLE_MOUNT_ROOTS = listOf("/run/media/", "/media/", "/mnt/")

/**
 * Linux counterpart of Android's detector: the home directory's filesystem stands in for internal
 * storage, and block devices mounted under the usual removable-media roots are the other volumes.
 */
@Singleton
actual class StorageVolumeDetector @Inject constructor() {
    actual fun detectStorageVolumes(): List<AppStorageVolume> {
        val internalPath = System.getProperty("user.home")
        val volumes = mutableListOf(createVolume(internalPath, "internal", "Internal", StorageVolumeType.INTERNAL))
        val addedPaths = mutableSetOf(internalPath)
        for (path in mountedVolumePaths()) {
            if (!addedPaths.add(path)) continue
            val type = if (path.contains("usb", ignoreCase = true)) StorageVolumeType.USB else StorageVolumeType.UNKNOWN
            volumes.add(createVolume(path, path, File(path).name, type))
        }
        return volumes
    }

    actual fun volumeRootPaths(): List<String> =
        listOf(System.getProperty("user.home")) + mountedVolumePaths()

    private fun mountedVolumePaths(): List<String> = try {
        File("/proc/mounts").readLines()
            .mapNotNull { line ->
                val parts = line.split(" ")
                if (parts.size >= 2 && parts[0].startsWith("/dev/")) decodeMountPath(parts[1]) else null
            }
            .filter { path -> REMOVABLE_MOUNT_ROOTS.any { path.startsWith(it) } }
            .filter { File(it).canRead() }
            .distinct()
    } catch (_: Exception) {
        emptyList()
    }

    private fun decodeMountPath(raw: String): String =
        raw.replace("\\040", " ").replace("\\011", "\t").replace("\\012", "\n").replace("\\134", "\\")

    private fun createVolume(path: String, id: String, displayName: String, type: StorageVolumeType): AppStorageVolume {
        val file = File(path)
        return AppStorageVolume(
            id = id,
            displayName = displayName,
            path = path,
            type = type,
            availableBytes = file.usableSpace,
            totalBytes = file.totalSpace
        )
    }
}
