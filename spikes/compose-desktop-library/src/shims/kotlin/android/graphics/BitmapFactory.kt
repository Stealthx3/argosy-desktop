package android.graphics

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/** android.graphics.Bitmap reduced to what upstream does with one: hand it to Compose. */
class Bitmap internal constructor(val image: ImageBitmap)

object BitmapFactory {
    fun decodeByteArray(data: ByteArray, offset: Int, length: Int): Bitmap? =
        runCatching { Bitmap(Image.makeFromEncoded(data.copyOfRange(offset, offset + length)).toComposeImageBitmap()) }.getOrNull()
}
