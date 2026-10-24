package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.nendo.argosy.platform.mpv.MpvPlayer
import com.nendo.argosy.platform.mpv.MpvVideoSurface
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import kotlinx.coroutines.delay
import java.io.File

@Composable
actual fun InlineTilePlayer(filePath: String, isPlaying: Boolean, isEngaged: Boolean, modifier: Modifier, isPaused: Boolean, seekTicks: Int, startPositionMs: Long, onPositionChanged: (Long) -> Unit, onTakeAudio: () -> Unit, onReleaseAudio: () -> Unit) {
    if (!File(filePath).canRead()) return
    val player = remember(filePath) { MpvPlayer.create(video = true, muted = true) } ?: return
    val reportPosition = rememberUpdatedState(onPositionChanged)
    val takeAudio = rememberUpdatedState(onTakeAudio)
    val releaseAudio = rememberUpdatedState(onReleaseAudio)

    DisposableEffect(player) {
        player.load(filePath, startPositionMs)
        onDispose {
            reportPosition.value(player.positionMs())
            player.release()
        }
    }
    LaunchedEffect(player, isPlaying, isPaused) { player.setPaused(!isPlaying || isPaused) }
    LaunchedEffect(player, isEngaged) {
        player.setMuted(!isEngaged)
        if (isEngaged) takeAudio.value() else releaseAudio.value()
    }
    LaunchedEffect(player, isPlaying) {
        while (isPlaying) {
            delay(ComponentDefaults.InlineTilePlayer.positionPollMs.toLong())
            reportPosition.value(player.positionMs())
        }
    }
    MpvVideoSurface(player, modifier)
}
