// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/ui/util/FocusModifiers.kt: focusBorder focusBackground
package com.nendo.argosy.ui.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.nendo.argosy.ui.theme.Motion

fun Modifier.focusBorder(
    isFocused: Boolean,
    color: Color,
    thickness: Dp,
    shape: Shape
): Modifier = if (isFocused) border(thickness, color, shape) else this

fun Modifier.focusBackground(
    isFocused: Boolean,
    focusedColor: Color,
    unfocusedColor: Color,
    shape: Shape
): Modifier = composed {
    val color by animateColorAsState(
        targetValue = if (isFocused) focusedColor else unfocusedColor,
        animationSpec = Motion.focusColorSpec,
        label = "focusBg"
    )
    background(color, shape)
}
