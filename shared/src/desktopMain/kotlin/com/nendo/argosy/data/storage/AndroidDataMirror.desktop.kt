package com.nendo.argosy.data.storage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class AndroidDataMirror @Inject constructor() {
    actual val isActive: Boolean = false

    actual fun isMirrorPath(path: String): Boolean = false

    actual fun realPathOf(mirrorPath: String): String? = null

    actual fun mirrorPathFor(path: String): String? = null

    actual fun sync(paths: Collection<String>): Boolean = false

    actual data class Entry(actual val isDirectory: Boolean, actual val size: Long, actual val mtimeSec: Long)

    actual companion object {}
}
