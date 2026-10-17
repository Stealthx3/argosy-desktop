package com.nendo.argosy.data.emulator

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class EmulatorDetector @Inject constructor() {
    actual val installedEmulators: StateFlow<List<InstalledEmulator>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual suspend fun detectEmulators(): List<InstalledEmulator> = emptyList()

    actual fun isInstalled(packageName: String): Boolean = false

    actual fun getInstalledForPlatform(platformId: String): List<InstalledEmulator> = emptyList()

    actual fun getPreferredEmulator(platformId: String, builtinEnabled: Boolean): InstalledEmulator? = null

    actual fun hasAnyEmulator(platformId: String): Boolean = false

    actual fun getByPackage(packageName: String): EmulatorDef? = null
}
