package com.nendo.argosy.ui.screens.settings.sections

import com.nendo.argosy.ui.screens.settings.ShaderStackState

internal fun shaderStackMaxFocusIndex(state: ShaderStackState): Int =
    (state.selectedShaderParams.size - 1).coerceAtLeast(0)
