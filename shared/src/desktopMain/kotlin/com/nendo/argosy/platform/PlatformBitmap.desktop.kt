package com.nendo.argosy.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
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

actual fun PlatformBitmap.setPixels(pixels: IntArray, offset: Int, stride: Int, x: Int, y: Int, width: Int, height: Int) {
    val bytes = ByteArray(width * height * 4)
    for (row in 0 until height) {
        for (col in 0 until width) {
            val argb = pixels[offset + row * stride + col]
            val i = (row * width + col) * 4
            bytes[i] = (argb and 0xFF).toByte()
            bytes[i + 1] = (argb shr 8 and 0xFF).toByte()
            bytes[i + 2] = (argb shr 16 and 0xFF).toByte()
            bytes[i + 3] = (argb ushr 24).toByte()
        }
    }
    val region = Bitmap().apply {
        allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL))
        installPixels(bytes)
    }
    val replace = Paint().apply { blendMode = BlendMode.SRC }
    Canvas(skiaBitmap).use { it.drawImage(Image.makeFromBitmap(region), x.toFloat(), y.toFloat(), replace) }
    region.close()
}

actual object PlatformBitmaps {
    actual fun createBitmap(width: Int, height: Int, config: PlatformBitmapConfig): PlatformBitmap =
        PlatformBitmap(Bitmap().apply { allocN32Pixels(width, height) })
}

actual object PlatformBitmapFactory {
    actual class Options actual constructor() {
        actual var inJustDecodeBounds: Boolean = false
        actual var inSampleSize: Int = 1
        internal var decodedWidth: Int = -1
        internal var decodedHeight: Int = -1

        actual val outWidth: Int get() = decodedWidth
        actual val outHeight: Int get() = decodedHeight
    }

    actual fun decodeFile(pathName: String): PlatformBitmap? = decodeFile(pathName, null)

    actual fun decodeByteArray(data: ByteArray, offset: Int, length: Int): PlatformBitmap? =
        decodeByteArray(data, offset, length, null)

    actual fun decodeByteArray(data: ByteArray, offset: Int, length: Int, opts: Options?): PlatformBitmap? {
        val bytes = data.copyOfRange(offset, offset + length)
        if (opts?.inJustDecodeBounds == true) {
            runCatching { Codec.makeFromData(Data.makeFromBytes(bytes)) }.getOrNull()?.use { codec ->
                opts.decodedWidth = codec.width
                opts.decodedHeight = codec.height
            }
            return null
        }
        val image = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return null
        return image.use { decoded ->
            val sample = opts?.inSampleSize?.coerceAtLeast(1) ?: 1
            val bitmap = if (sample == 1) {
                Bitmap.makeFromImage(decoded)
            } else {
                Bitmap().apply {
                    allocN32Pixels(maxOf(1, decoded.width / sample), maxOf(1, decoded.height / sample))
                    Canvas(this).use { canvas ->
                        canvas.drawImageRect(decoded, Rect.makeWH(width.toFloat(), height.toFloat()))
                    }
                }
            }
            PlatformBitmap(bitmap).also {
                opts?.decodedWidth = bitmap.width
                opts?.decodedHeight = bitmap.height
            }
        }
    }

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
