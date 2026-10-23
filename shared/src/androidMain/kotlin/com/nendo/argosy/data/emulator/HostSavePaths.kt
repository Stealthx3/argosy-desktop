package com.nendo.argosy.data.emulator

actual object HostSavePaths {
    actual fun saveDirectories(configId: String, templates: List<String>): List<String> = templates
    actual fun stateDirectories(configId: String, templates: List<String>): List<String> = templates
}
