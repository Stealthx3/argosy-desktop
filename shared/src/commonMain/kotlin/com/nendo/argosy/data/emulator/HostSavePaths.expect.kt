package com.nendo.argosy.data.emulator

/**
 * Where an emulator on this host keeps the save folder that a [SavePathConfig] names, and the
 * state folder that a [StatePathConfig] names, given the config's own templates. Android keeps
 * the templates; [SavePathRegistry] and [StatePathRegistry] own their expansion either way.
 */
expect object HostSavePaths {
    fun saveDirectories(configId: String, templates: List<String>): List<String>
    fun stateDirectories(configId: String, templates: List<String>): List<String>
}
