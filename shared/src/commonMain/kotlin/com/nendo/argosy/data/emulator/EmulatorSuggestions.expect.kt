package com.nendo.argosy.data.emulator

/**
 * Emulators this platform can install for a game platform. [isSupported] is false where the
 * emulator picker keeps upstream's recommended downloads; [forPlatform] lists the installable
 * emulators that support [canonicalSlug] and are not installed, and [install] installs one and
 * returns whether detection then finds it.
 */
expect class EmulatorSuggestions {
    val isSupported: Boolean

    fun forPlatform(canonicalSlug: String, installedPackages: Set<String>): List<EmulatorDef>

    suspend fun install(emulator: EmulatorDef): Boolean
}
