package com.nendo.argosy.data.emulator

data class InstalledEmulator(
    val def: EmulatorDef,
    val versionName: String?,
    val versionCode: Long
)
