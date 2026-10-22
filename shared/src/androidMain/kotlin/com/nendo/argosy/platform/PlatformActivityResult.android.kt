package com.nendo.argosy.platform

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class PlatformActivityResultContract<I, O>(val contract: ActivityResultContract<I, O>)

actual class PlatformActivityResultLauncher<I>(private val launcher: ActivityResultLauncher<I>) {
    actual fun launch(input: I) = launcher.launch(input)
}

actual object PlatformActivityResultContracts {
    actual fun OpenDocument(): PlatformActivityResultContract<Array<String>, PlatformUri?> =
        PlatformActivityResultContract(ActivityResultContracts.OpenDocument())

    actual fun RequestPermission(): PlatformActivityResultContract<String, Boolean> =
        PlatformActivityResultContract(ActivityResultContracts.RequestPermission())

    actual fun RequestMultiplePermissions(): PlatformActivityResultContract<Array<String>, Map<String, Boolean>> =
        PlatformActivityResultContract(ActivityResultContracts.RequestMultiplePermissions())
}

@Composable
actual fun <I, O> rememberLauncherForActivityResult(
    contract: PlatformActivityResultContract<I, O>,
    onResult: (O) -> Unit
): PlatformActivityResultLauncher<I> {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(contract.contract, onResult)
    return remember(launcher) { PlatformActivityResultLauncher(launcher) }
}
