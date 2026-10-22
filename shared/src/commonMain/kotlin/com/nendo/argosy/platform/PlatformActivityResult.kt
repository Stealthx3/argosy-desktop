package com.nendo.argosy.platform

import androidx.compose.runtime.Composable

expect class PlatformActivityResultContract<I, O>

expect class PlatformActivityResultLauncher<I> {
    fun launch(input: I)
}

expect object PlatformActivityResultContracts {
    fun OpenDocument(): PlatformActivityResultContract<Array<String>, PlatformUri?>
    fun RequestPermission(): PlatformActivityResultContract<String, Boolean>
    fun RequestMultiplePermissions(): PlatformActivityResultContract<Array<String>, Map<String, Boolean>>
}

@Composable
expect fun <I, O> rememberLauncherForActivityResult(
    contract: PlatformActivityResultContract<I, O>,
    onResult: (O) -> Unit
): PlatformActivityResultLauncher<I>
