package com.nendo.argosy.data.repository

import java.io.File

/**
 * Removes the folders left empty once [deleted] are gone, walking up from each path and stopping
 * at the first folder that still holds something or at a folder in [roots]. A root is removed only
 * when it is also in [removableRoots] and empty. Paths outside every root are left alone.
 */
internal fun pruneEmptyParents(deleted: List<File>, roots: Set<File>, removableRoots: Set<File>) {
    for (file in deleted) {
        if (roots.none { file.startsWith(it) }) continue
        var folder = file.parentFile
        while (folder != null && folder !in roots && folder.deleteIfEmptyFolder()) {
            folder = folder.parentFile
        }
    }
    removableRoots.forEach { it.deleteIfEmptyFolder() }
}

private fun File.deleteIfEmptyFolder(): Boolean =
    isDirectory && list()?.isEmpty() == true && delete()
