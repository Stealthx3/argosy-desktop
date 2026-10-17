package com.nendo.argosy.ui.input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

val LocalGamepadInputHandler = staticCompositionLocalOf<RawInputInterceptor?> { null }
