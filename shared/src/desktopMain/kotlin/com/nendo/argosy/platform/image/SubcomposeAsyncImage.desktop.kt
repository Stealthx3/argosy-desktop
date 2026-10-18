package com.nendo.argosy.platform.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImageScope

@Composable
actual fun SubcomposeAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier,
    error: (@Composable () -> Unit)?,
) {
    val platformModel = remember(model) { platformImageModel(model) }
    val errorContent: (@Composable SubcomposeAsyncImageScope.(AsyncImagePainter.State.Error) -> Unit)? =
        error?.let { content -> { content() } }
    coil3.compose.SubcomposeAsyncImage(
        model = platformModel,
        contentDescription = contentDescription,
        modifier = modifier,
        error = errorContent,
    )
}
