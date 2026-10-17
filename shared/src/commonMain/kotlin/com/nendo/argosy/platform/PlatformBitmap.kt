package com.nendo.argosy.platform

import androidx.compose.ui.graphics.ImageBitmap

expect class PlatformBitmap

expect enum class PlatformBitmapConfig {
    ARGB_8888,
    HARDWARE
}

expect val PlatformBitmap.width: Int

expect val PlatformBitmap.height: Int

expect val PlatformBitmap.config: PlatformBitmapConfig?

expect fun PlatformBitmap.getPixel(x: Int, y: Int): Int

expect fun PlatformBitmap.copy(config: PlatformBitmapConfig, isMutable: Boolean): PlatformBitmap?

expect fun PlatformBitmap.recycle()

expect fun PlatformBitmap.asImageBitmap(): ImageBitmap

expect object PlatformBitmapFactory {
    class Options() {
        var inJustDecodeBounds: Boolean
        val outWidth: Int
        val outHeight: Int
    }

    fun decodeFile(pathName: String): PlatformBitmap?

    fun decodeFile(pathName: String, opts: Options?): PlatformBitmap?
}
