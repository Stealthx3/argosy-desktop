package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

@Composable
actual fun Box3dCover(frontPath: String, spinePath: String, backPath: String?, modifier: Modifier, isInteractive: Boolean, onUnavailable: () -> Unit) {
    val reportUnavailable by rememberUpdatedState(onUnavailable)
    LaunchedEffect(frontPath, spinePath, backPath) {
        reportUnavailable()
    }
}
