package com.nendo.argosy.data.storage

expect class AndroidDataMirror {
    val isActive: Boolean

    fun isMirrorPath(path: String): Boolean

    fun realPathOf(mirrorPath: String): String?

    fun mirrorPathFor(path: String): String?

    fun sync(paths: Collection<String>): Boolean
}
