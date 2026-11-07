package com.nendo.argosy.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalActiveGamePalette
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.theme.LocalMotionTier
import com.nendo.argosy.ui.theme.Motion
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import kotlinx.coroutines.launch

/**
 * The hover wash [clickableNoFocus] draws on a control with no focus look of its own; null where
 * the display profile has no hover.
 */
val LocalHoverHighlight = staticCompositionLocalOf<Indication?> { null }

/**
 * Whether the pointer is over the app window. Every hover look is cleared while it is not.
 */
val LocalPointerInWindow = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(true) }

@Composable
fun rememberHoverHighlight(): Indication {
    val tint = LocalActiveGamePalette.current.firstOrNull() ?: LocalArgosyTheme.current.focusAccent
    val radius = Dimens.radiusControl
    val spring = Motion.tierFocusSpring(LocalMotionTier.current)
    val pointerInWindow = LocalPointerInWindow.current
    return remember(tint, radius, spring, pointerInWindow) { HoverHighlightIndication(tint, radius, spring, pointerInWindow) }
}

/**
 * Hover state for a component that draws its gamepad focus look while hovered. Pass it to
 * [clickableNoFocus] in place of an interaction source; [isHovered] stays false where the display
 * profile has no hover.
 */
@Stable
class HoverFocus internal constructor(
    val interactionSource: MutableInteractionSource,
    private val hovered: State<Boolean>,
    private val pointerInWindow: State<Boolean>,
    private val enabled: Boolean
) {
    val isHovered: Boolean get() = enabled && pointerInWindow.value && hovered.value
}

@Composable
fun rememberHoverFocus(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): HoverFocus {
    val enabled = LocalHoverHighlight.current != null
    val hovered = interactionSource.collectIsHoveredAsState()
    val pointerInWindow = LocalPointerInWindow.current
    return remember(interactionSource, hovered, pointerInWindow, enabled) {
        HoverFocus(interactionSource, hovered, pointerInWindow, enabled)
    }
}

private data class HoverHighlightIndication(
    val tint: Color,
    val radius: Dp,
    val spring: AnimationSpec<Float>,
    val pointerInWindow: State<Boolean>
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        HoverHighlightNode(interactionSource, tint, radius, spring, pointerInWindow)
}

private class HoverHighlightNode(
    private val source: InteractionSource,
    private val tint: Color,
    private val radius: Dp,
    private val spring: AnimationSpec<Float>,
    private val pointerInWindow: State<Boolean>
) : Modifier.Node(), DrawModifierNode {
    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> animateTo(1f)
                    is HoverInteraction.Exit -> animateTo(0f)
                }
            }
        }
    }

    private fun animateTo(target: Float) {
        coroutineScope.launch { progress.animateTo(target, spring) }
    }

    override fun ContentDrawScope.draw() {
        val amount = progress.value.coerceIn(0f, 1f)
        if (amount > 0f && pointerInWindow.value) {
            drawRoundRect(
                color = tint,
                alpha = ComponentDefaults.HoverHighlight.fillAlpha * amount,
                cornerRadius = CornerRadius(radius.toPx())
            )
        }
        drawContent()
    }
}
