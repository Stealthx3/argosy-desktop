package com.nendo.argosy.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.theme.Motion
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import kotlinx.coroutines.launch

/**
 * The mouse hover highlight drawn by [clickableNoFocus]; null where the display profile has none.
 */
val LocalHoverHighlight = staticCompositionLocalOf<Indication?> { null }

@Composable
fun rememberHoverHighlight(): Indication {
    val accent = LocalArgosyTheme.current.focusAccent
    val radius = Dimens.radiusControl
    val ring = Dimens.borderMedium
    return remember(accent, radius, ring) { HoverHighlightIndication(accent, radius, ring) }
}

private data class HoverHighlightIndication(
    val accent: Color,
    val radius: Dp,
    val ring: Dp
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        HoverHighlightNode(interactionSource, accent, radius, ring)
}

private class HoverHighlightNode(
    private val source: InteractionSource,
    private val accent: Color,
    private val radius: Dp,
    private val ring: Dp
) : Modifier.Node(), DrawModifierNode {
    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> fadeTo(1f)
                    is HoverInteraction.Exit -> fadeTo(0f)
                }
            }
        }
    }

    private fun fadeTo(target: Float) {
        coroutineScope.launch {
            progress.animateTo(target, tween(Motion.durationContent, easing = Motion.argosyEase))
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val amount = progress.value
        if (amount <= 0f) return
        val corner = CornerRadius(radius.toPx())
        val stroke = ring.toPx()
        drawRoundRect(
            color = accent,
            alpha = ComponentDefaults.HoverHighlight.fillAlpha * amount,
            cornerRadius = corner
        )
        drawRoundRect(
            color = accent,
            alpha = ComponentDefaults.HoverHighlight.ringAlpha * amount,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = corner,
            style = Stroke(width = stroke)
        )
    }
}
