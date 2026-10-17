package com.nendo.argosy.platform.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter

@Composable
actual fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier,
    placeholder: Painter?,
    error: Painter?,
    fallback: Painter?,
    onError: ((Any) -> Unit)?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    filterQuality: FilterQuality,
) {
    val errorCallback = remember(onError) {
        onError?.let { callback -> { state: AsyncImagePainter.State.Error -> callback(state) } }
    }
    coil.compose.AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        onError = errorCallback,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
    )
}
