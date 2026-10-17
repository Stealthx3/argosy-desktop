package com.nendo.argosy.ui.theme.backdrop

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.surfaceBackdrop(role: BackdropRole): Modifier =
    background(MaterialTheme.colorScheme.background)
