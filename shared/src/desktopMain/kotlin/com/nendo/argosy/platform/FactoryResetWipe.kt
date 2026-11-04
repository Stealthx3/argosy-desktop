package com.nendo.argosy.platform

import java.io.File

/**
 * The on-disk half of a factory reset. The running app schedules it and exits with
 * [RESTART_EXIT_CODE]; the launcher script starts the app again and the next process wipes the
 * data and cache folders before it opens any of them. An unfinished wipe runs again at the next
 * start, since the marker goes last.
 */
object FactoryResetWipe {
    const val RESTART_EXIT_CODE = 75

    private val marker: File
        get() = DesktopDirectories.data.resolveSibling("argosy-desktop-factory-reset")

    fun schedule() {
        marker.parentFile?.mkdirs()
        marker.writeText("")
    }

    fun runIfPending() {
        wipe(marker, listOf(DesktopDirectories.data, DesktopDirectories.cache))
    }

    internal fun wipe(marker: File, folders: List<File>): Boolean {
        if (!marker.exists()) return false
        if (folders.all { it.deleteRecursively() }) marker.delete()
        return true
    }
}
