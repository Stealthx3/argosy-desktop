package com.nendo.argosy.data.emulator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class EmulatorDetector {
    val installedEmulators: StateFlow<List<InstalledEmulator>>

    suspend fun detectEmulators(): List<InstalledEmulator>

    fun isInstalled(packageName: String): Boolean

    fun getInstalledForPlatform(platformId: String): List<InstalledEmulator>

    fun getPreferredEmulator(platformId: String, builtinEnabled: Boolean = true): InstalledEmulator?

    fun hasAnyEmulator(platformId: String): Boolean

    fun getByPackage(packageName: String): EmulatorDef?
}
