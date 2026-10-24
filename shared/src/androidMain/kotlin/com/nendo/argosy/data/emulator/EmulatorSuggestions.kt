package com.nendo.argosy.data.emulator

import javax.inject.Inject

actual class EmulatorSuggestions @Inject constructor() {
    actual val isSupported: Boolean = false

    actual fun forPlatform(canonicalSlug: String, installedPackages: Set<String>): List<EmulatorDef> = emptyList()

    actual suspend fun install(emulator: EmulatorDef): Boolean = false
}
