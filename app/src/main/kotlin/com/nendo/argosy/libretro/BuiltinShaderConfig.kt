package com.nendo.argosy.libretro

import com.nendo.argosy.data.preferences.BuiltinEmulatorSettings

val BuiltinEmulatorSettings.shaderConfig: com.swordfish.libretrodroid.ShaderConfig
    get() = when (shader) {
        "CRT" -> com.swordfish.libretrodroid.ShaderConfig.CRT
        "LCD" -> com.swordfish.libretrodroid.ShaderConfig.LCD
        "Sharp" -> com.swordfish.libretrodroid.ShaderConfig.Sharp
        "CUT" -> com.swordfish.libretrodroid.ShaderConfig.CUT()
        "CUT2" -> com.swordfish.libretrodroid.ShaderConfig.CUT2()
        "CUT3" -> com.swordfish.libretrodroid.ShaderConfig.CUT3()
        "Custom" -> com.swordfish.libretrodroid.ShaderConfig.Default
        else -> com.swordfish.libretrodroid.ShaderConfig.Default
    }
