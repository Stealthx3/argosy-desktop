package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun YouTubeVideoPlayer(videoId: String, muted: Boolean, onReady: () -> Unit, onError: () -> Unit, modifier: Modifier = Modifier)
