package com.nendo.argosy.platform.image

import coil3.PlatformContext
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.FileSystem
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmoothImageDecoderTest {
    private fun options(width: Int, height: Int, precision: Precision = Precision.INEXACT) = Options(
        context = PlatformContext.INSTANCE,
        size = Size(width, height),
        scale = Scale.FILL,
        precision = precision
    )

    @Test
    fun inexactRequestsKeepAPowerOfTwoSampleAtLeastTheTargetSize() {
        assertEquals(0.25, SmoothImageDecoder.decodeMultiplier(1238, 2000, options(300, 480)))
        assertEquals(0.5, SmoothImageDecoder.decodeMultiplier(1238, 2000, options(400, 640)))
        assertEquals(1.0, SmoothImageDecoder.decodeMultiplier(200, 300, options(400, 640)))
    }

    @Test
    fun exactRequestsScaleToTheTarget() {
        assertEquals(0.24, SmoothImageDecoder.decodeMultiplier(1250, 2000, options(300, 480, Precision.EXACT)))
    }

    @Test
    fun finePatternsAverageInsteadOfAliasing() = runBlocking {
        val stripes = Bitmap().apply { allocN32Pixels(800, 800) }
        Canvas(stripes).use { canvas ->
            canvas.clear(Color.WHITE)
            val black = Paint().apply { color = Color.BLACK }
            for (x in 0 until 800 step 2) canvas.drawRect(Rect.makeXYWH(x.toFloat(), 0f, 1f, 800f), black)
        }
        val png = Image.makeFromBitmap(stripes).encodeToData(EncodedImageFormat.PNG)!!.bytes
        val source = ImageSource(Buffer().write(png), FileSystem.SYSTEM)
        val result = SmoothImageDecoder(source, options(100, 100, Precision.EXACT)).decode()
        val bitmap = (result.image as coil3.BitmapImage).bitmap
        assertEquals(100, bitmap.width)
        val greys = (0 until 100).map { Color.getR(bitmap.getColor(it, 50)) }
        assertTrue(greys.all { it in 90..165 }, greys.toString())
    }
}
