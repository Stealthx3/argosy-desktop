package com.nendo.argosy.platform.image

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.isOriginal
import coil3.size.pxOrElse
import coil3.svg.isSvg
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import kotlin.math.roundToInt

class SmoothImageDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val image = Image.makeFromEncoded(bytes)
        try {
            val multiplier = decodeMultiplier(image.width, image.height, options)
            val width = (image.width * multiplier).roundToInt().coerceAtLeast(1)
            val height = (image.height * multiplier).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap()
            bitmap.allocN32Pixels(width, height)
            Canvas(bitmap).use { canvas ->
                canvas.drawImageRect(
                    image,
                    Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                    Rect.makeWH(width.toFloat(), height.toFloat()),
                    DOWNSCALE_SAMPLING,
                    null,
                    false
                )
            }
            bitmap.setImmutable()
            return DecodeResult(
                image = bitmap.asImage(),
                isSampled = width < image.width || height < image.height
            )
        } finally {
            image.close()
        }
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (result.mimeType == SVG_MIME_TYPE || DecodeUtils.isSvg(result.source.source())) return null
            return SmoothImageDecoder(result.source, options)
        }
    }

    companion object {
        private const val SVG_MIME_TYPE = "image/svg+xml"
        private val DOWNSCALE_SAMPLING: SamplingMode = FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR)

        fun decodeMultiplier(srcWidth: Int, srcHeight: Int, options: Options): Double {
            val size = options.size
            val dstWidth = if (size.isOriginal) srcWidth else size.width.pxOrElse { undefinedDimension(options.scale) }
            val dstHeight = if (size.isOriginal) srcHeight else size.height.pxOrElse { undefinedDimension(options.scale) }
            val exact = DecodeUtils.computeSizeMultiplier(srcWidth, srcHeight, dstWidth, dstHeight, options.scale)
            val sampled = when {
                options.precision == Precision.EXACT -> exact
                exact >= 1.0 -> 1.0
                else -> 1.0 / (1.0 / exact).toInt().coerceAtLeast(1).takeHighestOneBit()
            }
            return minOf(sampled, maxSizeMultiplier(srcWidth, srcHeight, options))
        }

        private fun undefinedDimension(scale: Scale): Int = if (scale == Scale.FILL) Int.MIN_VALUE else Int.MAX_VALUE

        private fun maxSizeMultiplier(srcWidth: Int, srcHeight: Int, options: Options): Double {
            val max = options.maxBitmapSize
            val byWidth = (max.width as? Dimension.Pixels)?.let { it.px / srcWidth.toDouble() } ?: Double.MAX_VALUE
            val byHeight = (max.height as? Dimension.Pixels)?.let { it.px / srcHeight.toDouble() } ?: Double.MAX_VALUE
            return minOf(byWidth, byHeight)
        }
    }
}
