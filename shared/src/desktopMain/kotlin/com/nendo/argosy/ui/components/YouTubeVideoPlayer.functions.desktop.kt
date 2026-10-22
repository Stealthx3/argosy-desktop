package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

@Composable
actual fun YouTubeVideoPlayer(videoId: String, muted: Boolean, onReady: () -> Unit, onError: () -> Unit, modifier: Modifier) {
    val reportError by rememberUpdatedState(onError)
    LaunchedEffect(videoId) {
        reportError()
    }
}
