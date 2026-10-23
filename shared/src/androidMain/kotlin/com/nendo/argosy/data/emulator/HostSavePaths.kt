package com.nendo.argosy.data.emulator

actual object HostSavePaths {
    actual fun saveDirectories(configId: String): List<String>? = null
    actual fun stateDirectories(configId: String): List<String>? = null
}
