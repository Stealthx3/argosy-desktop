package com.nendo.argosy.ui.components

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.nendo.argosy.platform.config
import com.nendo.argosy.platform.copy
import com.nendo.argosy.platform.getPixel
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.recycle
import com.nendo.argosy.platform.setPixels
import com.nendo.argosy.platform.width
import com.nendo.argosy.platform.PlatformBitmaps
import com.nendo.argosy.platform.PlatformBitmapConfig
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nendo.argosy.platform.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.res.ArgosyR as R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun QrCodeWithOverlay(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    overlayText: String? = null
) {
    val qrBitmap = remember(data) {
        generateQrBitmap(data, size.value.toInt() * 3)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        qrBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.ui_qr_code_description, data),
                modifier = Modifier.size(size)
            )
        }

        if (overlayText != null) {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = overlayText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.Black
                )
            }
        }
    }
}

private fun generateQrBitmap(data: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        val black = Color.Black.toArgb()
        val white = Color.White.toArgb()

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) black else white
            }
        }

        PlatformBitmaps.createBitmap(width, height, PlatformBitmapConfig.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (e: Exception) {
        null
    }
}
