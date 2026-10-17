package com.nendo.argosy.data.emulator

import com.nendo.argosy.core.emulator.EmulatorDownloadState

data class EmulatorDownloadProgress(
    val emulatorId: String,
    val state: EmulatorDownloadState,
    val variant: String? = null
)
