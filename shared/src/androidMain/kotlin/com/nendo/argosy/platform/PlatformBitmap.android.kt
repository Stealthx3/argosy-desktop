package com.nendo.argosy.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap as composeImageBitmap

actual typealias PlatformBitmap = Bitmap

actual typealias PlatformBitmapConfig = Bitmap.Config

actual val PlatformBitmap.width: Int get() = width

actual val PlatformBitmap.height: Int get() = height

actual val PlatformBitmap.config: PlatformBitmapConfig? get() = config

actual fun PlatformBitmap.getPixel(x: Int, y: Int): Int = getPixel(x, y)

actual fun PlatformBitmap.copy(config: PlatformBitmapConfig, isMutable: Boolean): PlatformBitmap? =
    copy(config, isMutable)

actual fun PlatformBitmap.recycle() = recycle()

actual fun PlatformBitmap.asImageBitmap(): ImageBitmap = composeImageBitmap()

actual object PlatformBitmapFactory {
    actual class Options actual constructor() {
        internal val options = BitmapFactory.Options()

        actual var inJustDecodeBounds: Boolean
            get() = options.inJustDecodeBounds
            set(value) {
                options.inJustDecodeBounds = value
            }
        actual val outWidth: Int get() = options.outWidth
        actual val outHeight: Int get() = options.outHeight
    }

    actual fun decodeFile(pathName: String): PlatformBitmap? = BitmapFactory.decodeFile(pathName)

    actual fun decodeFile(pathName: String, opts: Options?): PlatformBitmap? =
        BitmapFactory.decodeFile(pathName, opts?.options)
}
