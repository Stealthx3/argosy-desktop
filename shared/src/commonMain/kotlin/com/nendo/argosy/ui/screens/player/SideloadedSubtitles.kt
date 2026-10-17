package com.nendo.argosy.ui.screens.player

/**
 * Identifies a subtitle attached alongside the video rather than carried inside it. The player has
 * no other way to tell one of these apart from a track the container already had, and picking the
 * wrong one silently shows the wrong language.
 */
fun sideloadedSubtitleId(streamIndex: Int): String = "argosy-sub-$streamIndex"
