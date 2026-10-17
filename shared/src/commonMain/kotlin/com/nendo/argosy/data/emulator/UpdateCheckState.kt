package com.nendo.argosy.data.emulator

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class Completed(val updatesFound: Int) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}
