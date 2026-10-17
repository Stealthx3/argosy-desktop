package com.nendo.argosy.ui.theme.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Modifier.surfaceBackdrop(role: BackdropRole): Modifier
