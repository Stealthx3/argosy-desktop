package com.nendo.argosy.ui.screens.settings.sections

import androidx.compose.runtime.Composable
import com.nendo.argosy.libretro.shader.ShaderChainManager

@Composable
expect fun ShaderStackSection(manager: ShaderChainManager)
