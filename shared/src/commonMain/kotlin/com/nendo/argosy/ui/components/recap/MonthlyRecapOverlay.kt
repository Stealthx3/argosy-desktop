package com.nendo.argosy.ui.components.recap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import com.nendo.argosy.platform.LocalPlatformContext as LocalContext
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nendo.argosy.ui.components.FooterHints
import com.nendo.argosy.ui.components.launchOverlayFadeMs
import com.nendo.argosy.ui.input.ModalInputEffect
import com.nendo.argosy.ui.theme.LocalLauncherTheme
import com.nendo.argosy.ui.theme.Motion
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import com.nendo.argosy.ui.util.clickableNoFocus

@Composable
private fun rememberMonthlyRecapController(): MonthlyRecapController {
    val context = LocalContext.current
    return remember { monthlyRecapController(context) }
}

/**
 * The monthly recap over the control surface. Holds the input stack while open.
 */
@Composable
fun MonthlyRecapOverlay(modifier: Modifier = Modifier) {
    val controller = rememberMonthlyRecapController()
    val state by controller.state.collectAsState()
    val cursor by controller.cursor.collectAsState()
    ModalInputEffect(active = state != null, handler = controller.inputHandler)
    if (state != null) FooterHints(hints = emptyList())
    MonthlyRecapContent(
        state = state,
        cursor = cursor,
        surface = RecapSurface.CONTROL,
        onClose = controller::close,
        onCurrent = controller::centered,
        modifier = modifier
    )
}

@Composable
fun MonthlyRecapMirror(modifier: Modifier = Modifier) {
    val controller = rememberMonthlyRecapController()
    val state by controller.state.collectAsState()
    val cursor by controller.cursor.collectAsState()
    MonthlyRecapContent(
        state = state,
        cursor = cursor,
        surface = RecapSurface.PRESENTATION,
        onClose = controller::close,
        onCurrent = controller::centered,
        modifier = modifier
    )
}

/**
 * The blur the screen behind the monthly recap takes while it shows, for the surface's content to
 * apply alongside its other modal blurs.
 */
@Composable
fun monthlyRecapBackdropBlur(): Dp {
    val controller = rememberMonthlyRecapController()
    val state by controller.state.collectAsState()
    val blur by animateDpAsState(
        targetValue = if (state != null) Motion.blurRadiusDrawer else 0.dp,
        animationSpec = tween(launchOverlayFadeMs()),
        label = "monthlyRecapBlur"
    )
    return blur
}

@Composable
fun monthlyRecapShowing(): Boolean {
    val controller = rememberMonthlyRecapController()
    val state by controller.state.collectAsState()
    return state != null
}

@Composable
private fun MonthlyRecapContent(
    state: MonthlyRecapCardState?,
    cursor: RecapCursor,
    surface: RecapSurface,
    onClose: () -> Unit,
    onCurrent: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastShown by remember { mutableStateOf<MonthlyRecapCardState?>(null) }
    SideEffect { state?.let { lastShown = it } }
    val scrimAlpha = if (LocalLauncherTheme.current.isDarkTheme) {
        ComponentDefaults.LaunchOverlay.scrimAlphaDark
    } else {
        ComponentDefaults.LaunchOverlay.scrimAlphaLight
    }
    val scrimColor = MaterialTheme.colorScheme.background.copy(alpha = scrimAlpha)
    val fadeMs = launchOverlayFadeMs()
    AnimatedVisibility(
        visible = state != null,
        enter = fadeIn(tween(fadeMs)),
        exit = fadeOut(tween(fadeMs)),
        modifier = modifier.fillMaxSize().focusProperties { canFocus = false }
    ) {
        val shown = state ?: lastShown ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickableNoFocus { },
            contentAlignment = Alignment.Center
        ) {
            if (rememberRecapFullCardSurface() == surface) {
                MonthlyRecapCard(state = shown, cursor = cursor, onClose = onClose, onCurrent = onCurrent)
            } else {
                MonthlyRecapCompactCard(state = shown, onClose = onClose)
            }
        }
    }
}
