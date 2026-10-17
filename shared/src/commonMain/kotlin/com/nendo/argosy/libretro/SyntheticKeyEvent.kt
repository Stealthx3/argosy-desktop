package com.nendo.argosy.libretro

data class SyntheticKeyEvent(
    val keyCode: Int,
    val action: Int,
    val port: Int,
    val axis: Int
)
