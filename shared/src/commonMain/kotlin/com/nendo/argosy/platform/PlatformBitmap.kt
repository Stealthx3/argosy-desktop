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

expect fun PlatformBitmap.setPixels(pixels: IntArray, offset: Int, stride: Int, x: Int, y: Int, width: Int, height: Int)

expect object PlatformBitmaps {
    fun createBitmap(width: Int, height: Int, config: PlatformBitmapConfig): PlatformBitmap
}

expect object PlatformBitmapFactory {
    class Options() {
        var inJustDecodeBounds: Boolean
        var inSampleSize: Int
        val outWidth: Int
        val outHeight: Int
    }

    fun decodeFile(pathName: String): PlatformBitmap?

    fun decodeFile(pathName: String, opts: Options?): PlatformBitmap?

    fun decodeByteArray(data: ByteArray, offset: Int, length: Int): PlatformBitmap?

    fun decodeByteArray(data: ByteArray, offset: Int, length: Int, opts: Options?): PlatformBitmap?
}
