package com.nendo.argosy.libretro.shader

import com.nendo.argosy.ui.screens.settings.ShaderStackState

actual val ShaderChainManager.shaderStack: ShaderStackState
    get() = ShaderStackState()
