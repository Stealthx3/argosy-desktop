package com.nendo.argosy.platform

import android.os.StatFs

actual class PlatformStatFs actual constructor(path: String) {
    private val stat = StatFs(path)

    actual val availableBytes: Long get() = stat.availableBytes
}
