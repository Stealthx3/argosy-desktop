package com.nendo.argosy.libretro.shader

import com.nendo.argosy.ui.screens.settings.ShaderStackState

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual val ShaderChainManager.shaderStack: ShaderStackState
    get() = this.shaderStack
