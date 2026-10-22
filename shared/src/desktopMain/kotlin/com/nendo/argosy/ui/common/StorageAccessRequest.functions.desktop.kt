package com.nendo.argosy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberStorageAccessRequest(onResult: () -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember { { currentOnResult() } }
}
