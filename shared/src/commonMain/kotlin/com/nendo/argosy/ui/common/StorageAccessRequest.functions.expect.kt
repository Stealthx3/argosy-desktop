package com.nendo.argosy.ui.common

import androidx.compose.runtime.Composable

@Composable
expect fun rememberStorageAccessRequest(onResult: () -> Unit = {}): () -> Unit
