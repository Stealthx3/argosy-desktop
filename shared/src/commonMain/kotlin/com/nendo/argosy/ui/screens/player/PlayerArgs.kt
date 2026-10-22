package com.nendo.argosy.ui.screens.player

/**
 * How the player was asked to open. [startPositionMs] below zero means the caller made no decision
 * and the player resolves resume itself, which is what a relaunch on another display sends.
 */
data class PlayerArgs(
    val itemId: String,
    val title: String = "",
    val subtitle: String = "",
    val startPositionMs: Long = -1
)
