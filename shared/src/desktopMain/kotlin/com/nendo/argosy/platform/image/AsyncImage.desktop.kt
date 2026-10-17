package com.nendo.argosy.platform.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter
import coil3.toBitmap

@Composable
actual fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier,
    placeholder: Painter?,
    error: Painter?,
    fallback: Painter?,
    onError: ((Any) -> Unit)?,
    onSuccess: ((AsyncImageSuccess) -> Unit)?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    filterQuality: FilterQuality,
) {
    val platformModel = remember(model) { platformImageModel(model) }
    val successCallback = remember(onSuccess) {
        onSuccess?.let { callback -> { state: AsyncImagePainter.State.Success -> callback(AsyncImageSuccess(AsyncImageSuccessResult(PlatformBitmapDrawable(com.nendo.argosy.platform.PlatformBitmap(state.result.image.toBitmap()))))) } }
    }
    val errorCallback = remember(onError) {
        onError?.let { callback -> { state: AsyncImagePainter.State.Error -> callback(state) } }
    }
    coil3.compose.AsyncImage(
        model = platformModel,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        onError = errorCallback,
        onSuccess = successCallback,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
    )
}
