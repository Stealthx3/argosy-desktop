// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/ui/theme/backdrop/SurfaceBackdrop.kt: LocalSurfaceBackdrop
package com.nendo.argosy.ui.theme.backdrop

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSurfaceBackdrop = staticCompositionLocalOf { BackdropConfig() }
