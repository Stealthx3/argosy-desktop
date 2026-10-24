package com.nendo.argosy.platform.mpv

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged

@Composable
fun MpvVideoSurface(player: MpvPlayer, modifier: Modifier = Modifier) {
    val frame by player.frame.collectAsState()
    Canvas(modifier = modifier.onSizeChanged { player.resize(it.width, it.height) }) {
        if (frame >= 0) player.drawFront { drawImage(it.asComposeImageBitmap()) }
    }
}
