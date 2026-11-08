package com.nendo.argosy.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nendo.argosy.data.preferences.DisplayProfile
import com.nendo.argosy.ui.theme.LocalActiveGamePalette
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.theme.LocalMotionTier
import com.nendo.argosy.ui.theme.Motion
import com.nendo.argosy.ui.theme.generated.ComponentDefaults

/**
 * The display profile the app is shown with; null where Argosy has no display profiles.
 */
val LocalDisplayProfile = staticCompositionLocalOf<DisplayProfile?> { null }

val isTvLayout: Boolean
    @Composable @ReadOnlyComposable get() = LocalDisplayProfile.current == DisplayProfile.TV

/**
 * The thinnest a divider or outline is drawn: [ComponentDefaults.TvLayout.lineMinDp] on the TV
 * layout, [width] elsewhere.
 */
@Composable
@ReadOnlyComposable
fun tvLineWidth(width: Dp): Dp =
    if (isTvLayout) maxOf(width, ComponentDefaults.TvLayout.lineMinDp.dp) else width

/**
 * On the TV layout, an accent glow drawn out from behind the edges of [shape] while [focused];
 * nothing elsewhere.
 */
fun Modifier.tvFocusGlow(focused: Boolean, shape: Shape, tint: Color? = null): Modifier = composed {
    if (!isTvLayout) return@composed this
    val accent = tint ?: LocalActiveGamePalette.current.firstOrNull() ?: LocalArgosyTheme.current.focusAccent
    val amount by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = Motion.tierFocusSpring(LocalMotionTier.current),
        label = "tv-focus-glow"
    )
    drawBehind {
        if (amount > 0f) drawEdgeGlow(shape, accent, amount)
    }
}

private fun DrawScope.drawEdgeGlow(shape: Shape, accent: Color, amount: Float) {
    val radius = ComponentDefaults.TvLayout.glowRadiusDp.dp.toPx()
    val steps = GLOW_STEPS
    for (step in steps downTo 1) {
        val spread = radius * step / steps
        val grown = Size(size.width + spread * 2, size.height + spread * 2)
        val outline = shape.createOutline(grown, layoutDirection, this)
        val falloff = 1f - (step - 1).toFloat() / steps
        val alpha = ComponentDefaults.TvLayout.glowAlpha * amount * falloff * falloff / steps * 2f
        translate(-spread, -spread) {
            drawOutlineStroke(outline, accent.copy(alpha = alpha.coerceIn(0f, 1f)), radius / steps * 1.5f)
        }
    }
}

private fun DrawScope.drawOutlineStroke(outline: Outline, color: Color, width: Float) {
    val stroke = Stroke(width = width)
    when (outline) {
        is Outline.Rectangle -> drawRect(color, outline.rect.topLeft, outline.rect.size, style = stroke)
        is Outline.Rounded -> drawPath(
            Path().apply { addRoundRect(outline.roundRect) },
            color,
            style = stroke
        )
        is Outline.Generic -> drawPath(outline.path, color, style = stroke)
    }
}

private const val GLOW_STEPS = 6
