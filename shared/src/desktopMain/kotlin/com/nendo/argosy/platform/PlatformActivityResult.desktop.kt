package com.nendo.argosy.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

actual class PlatformActivityResultContract<I, O>(internal val resolve: suspend (I) -> O)

actual class PlatformActivityResultLauncher<I>(private val onLaunch: (I) -> Unit) {
    actual fun launch(input: I) = onLaunch(input)
}

actual object PlatformActivityResultContracts {
    actual fun OpenDocument(): PlatformActivityResultContract<Array<String>, PlatformUri?> =
        PlatformActivityResultContract { mimeTypes -> PortalFileChooser.openFile(mimeTypes) }

    actual fun RequestPermission(): PlatformActivityResultContract<String, Boolean> =
        PlatformActivityResultContract { true }

    actual fun RequestMultiplePermissions(): PlatformActivityResultContract<Array<String>, Map<String, Boolean>> =
        PlatformActivityResultContract { permissions -> permissions.associateWith { true } }
}

@Composable
actual fun <I, O> rememberLauncherForActivityResult(
    contract: PlatformActivityResultContract<I, O>,
    onResult: (O) -> Unit
): PlatformActivityResultLauncher<I> {
    val currentContract by rememberUpdatedState(contract)
    val currentOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    return remember(scope) {
        PlatformActivityResultLauncher { input -> scope.launch { currentOnResult(currentContract.resolve(input)) } }
    }
}
