package com.nendo.argosy.platform

expect class PlatformStatFs(path: String) {
    val availableBytes: Long
}
