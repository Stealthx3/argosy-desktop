package com.nendo.argosy.util

data class AttachedScreen(
    val key: String,
    val displayId: Int,
    val number: Int,
    val widthPx: Int,
    val heightPx: Int,
    val builtIn: Boolean
)
