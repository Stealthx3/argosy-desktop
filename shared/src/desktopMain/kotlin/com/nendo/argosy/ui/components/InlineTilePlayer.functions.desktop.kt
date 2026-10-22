package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun InlineTilePlayer(filePath: String, isPlaying: Boolean, isEngaged: Boolean, modifier: Modifier, isPaused: Boolean, seekTicks: Int, startPositionMs: Long, onPositionChanged: (Long) -> Unit, onTakeAudio: () -> Unit, onReleaseAudio: () -> Unit) {
}
