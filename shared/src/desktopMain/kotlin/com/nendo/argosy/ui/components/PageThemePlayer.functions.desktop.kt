package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.nendo.argosy.platform.mpv.MpvPlayer
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import kotlinx.coroutines.delay
import java.io.File

@Composable
actual fun PageThemePlayer(filePath: String?) {
    val readable = filePath?.takeIf { it.isNotBlank() && File(it).canRead() } ?: return
    val player = remember(readable) { MpvPlayer.create(video = false, muted = false) } ?: return
    DisposableEffect(player) {
        player.setVolume(0)
        player.load(readable)
        onDispose { player.release() }
    }
    LaunchedEffect(player) {
        val stepDelay = ComponentDefaults.InlineTilePlayer.audioFadeMs.toLong() / FADE_STEPS
        for (step in 1..FADE_STEPS) {
            player.setVolume(FULL_VOLUME * step / FADE_STEPS)
            delay(stepDelay)
        }
    }
}

private const val FADE_STEPS = 12
private const val FULL_VOLUME = 100
