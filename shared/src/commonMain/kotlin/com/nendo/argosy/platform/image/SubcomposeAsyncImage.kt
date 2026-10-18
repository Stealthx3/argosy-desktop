package com.nendo.argosy.platform.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun SubcomposeAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    error: (@Composable () -> Unit)? = null,
)
