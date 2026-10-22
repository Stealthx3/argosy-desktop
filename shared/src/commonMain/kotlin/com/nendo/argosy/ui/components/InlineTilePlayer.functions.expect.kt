package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun InlineTilePlayer(filePath: String, isPlaying: Boolean, isEngaged: Boolean, modifier: Modifier = Modifier, isPaused: Boolean = false, seekTicks: Int = 0, startPositionMs: Long = 0L, onPositionChanged: (Long) -> Unit = {}, onTakeAudio: () -> Unit = {}, onReleaseAudio: () -> Unit = {})
