package com.nendo.argosy.ui.components
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun deviceHasCamera(context: com.nendo.argosy.platform.PlatformContext): Boolean = false

@Composable
actual fun QrScannerWithPermission(onResult: (PairingQrResult) -> Unit, onDismiss: () -> Unit, modifier: Modifier) {
}
