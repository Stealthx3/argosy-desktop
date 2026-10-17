package com.nendo.argosy.platform

import java.io.File

actual class PlatformStatFs actual constructor(path: String) {
    private val file = File(path).also { require(it.exists()) { "Invalid path: $path" } }

    actual val availableBytes: Long get() = file.usableSpace
}
