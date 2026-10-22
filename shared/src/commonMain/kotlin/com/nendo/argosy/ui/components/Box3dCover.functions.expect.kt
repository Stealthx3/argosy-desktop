package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Box3dCover(frontPath: String, spinePath: String, backPath: String? = null, modifier: Modifier = Modifier, isInteractive: Boolean = true, onUnavailable: () -> Unit = {})
