package com.nendo.argosy.data.emulator

/**
 * Where a standalone emulator on this host keeps the save folder that a [SavePathConfig] names,
 * and the state folder that a [StatePathConfig] names. Null keeps the config's own templates;
 * [SavePathRegistry] and [StatePathRegistry] own their expansion either way.
 */
expect object HostSavePaths {
    fun saveDirectories(configId: String): List<String>?
    fun stateDirectories(configId: String): List<String>?
}
