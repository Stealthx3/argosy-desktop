package com.nendo.argosy.ui.theme.backdrop

import androidx.compose.runtime.staticCompositionLocalOf

/** Alpha-cap role of the surface the backdrop paints: CONTENT sits under menus, WALLPAPER under the companion screen. */
enum class BackdropRole { CONTENT, WALLPAPER }

val LocalSurfaceBackdrop = staticCompositionLocalOf { BackdropConfig() }
