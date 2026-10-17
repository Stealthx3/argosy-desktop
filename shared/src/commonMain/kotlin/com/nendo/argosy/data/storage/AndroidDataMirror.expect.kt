package com.nendo.argosy.data.storage

expect class AndroidDataMirror {
    class Entry {
        val isDirectory: Boolean
        val size: Long
        val mtimeSec: Long
    }

    companion object {}

    val isActive: Boolean

    fun isMirrorPath(path: String): Boolean

    fun realPathOf(mirrorPath: String): String?

    fun mirrorPathFor(path: String): String?

    fun sync(paths: Collection<String>): Boolean
}
