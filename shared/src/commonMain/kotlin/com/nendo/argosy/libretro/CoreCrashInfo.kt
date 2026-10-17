package com.nendo.argosy.libretro

data class CoreCrashInfo(
    val coreId: String,
    val gameId: Long,
    val reason: Int,
    val exitTimestamp: Long
)
