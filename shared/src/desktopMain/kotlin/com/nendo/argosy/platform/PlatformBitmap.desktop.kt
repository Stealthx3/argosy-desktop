package com.nendo.argosy.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import java.io.File

actual class PlatformBitmap(val skiaBitmap: Bitmap)

actual enum class PlatformBitmapConfig {
    ARGB_8888,
    HARDWARE
}

actual val PlatformBitmap.width: Int get() = skiaBitmap.width

actual val PlatformBitmap.height: Int get() = skiaBitmap.height

actual val PlatformBitmap.config: PlatformBitmapConfig? get() = PlatformBitmapConfig.ARGB_8888

actual fun PlatformBitmap.getPixel(x: Int, y: Int): Int = skiaBitmap.getColor(x, y)

actual fun PlatformBitmap.copy(config: PlatformBitmapConfig, isMutable: Boolean): PlatformBitmap? =
    skiaBitmap.makeClone()?.let(::PlatformBitmap)

actual fun PlatformBitmap.recycle() = skiaBitmap.close()

actual fun PlatformBitmap.asImageBitmap(): ImageBitmap = skiaBitmap.asComposeImageBitmap()

actual object PlatformBitmapFactory {
    actual class Options actual constructor() {
        actual var inJustDecodeBounds: Boolean = false
        internal var decodedWidth: Int = -1
        internal var decodedHeight: Int = -1

        actual val outWidth: Int get() = decodedWidth
        actual val outHeight: Int get() = decodedHeight
    }

    actual fun decodeFile(pathName: String): PlatformBitmap? = decodeFile(pathName, null)

    actual fun decodeFile(pathName: String, opts: Options?): PlatformBitmap? {
        val bytes = runCatching { File(pathName).readBytes() }.getOrNull() ?: return null
        if (opts?.inJustDecodeBounds == true) {
            runCatching { Codec.makeFromData(Data.makeFromBytes(bytes)) }.getOrNull()?.use { codec ->
                opts.decodedWidth = codec.width
                opts.decodedHeight = codec.height
            }
            return null
        }
        val image = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return null
        return image.use { PlatformBitmap(Bitmap.makeFromImage(it)) }.also { bitmap ->
            opts?.decodedWidth = bitmap.skiaBitmap.width
            opts?.decodedHeight = bitmap.skiaBitmap.height
        }
    }
}
