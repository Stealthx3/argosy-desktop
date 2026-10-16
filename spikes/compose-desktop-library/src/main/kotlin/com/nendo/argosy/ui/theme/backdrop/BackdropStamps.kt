package com.nendo.argosy.ui.theme.backdrop

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Stub of upstream's BackdropStampProvider (Hilt singleton that rasterises platform SVGs via Coil
 * into android.graphics.Bitmap masks). The PLATFORMS backdrop preset draws no stamps here.
 */
val LocalBackdropStamps = staticCompositionLocalOf<BackdropStampProvider?> { null }

fun backdropStampProvider(context: Context): BackdropStampProvider = BackdropStampProvider

object BackdropStampProvider {
    val stampUris: StateFlow<List<String>> = MutableStateFlow(emptyList())

    suspend fun loadStamps(uris: List<String>, sizePx: Int, tint: Color, alpha: Float): List<ImageBitmap> = emptyList()
}
