package com.nendo.argosy.data.emulator

/**
 * Where a standalone emulator on this host keeps the save folder that a [SavePathConfig] names.
 * Null keeps the config's own templates; [SavePathRegistry] owns their expansion either way.
 */
expect object HostSavePaths {
    fun saveDirectories(configId: String): List<String>?
}
