package com.nendo.argosy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.nendo.argosy.ui.theme.generated.ComponentDefaults

/**
 * The TV layout's typography: every style at least [ComponentDefaults.TvLayout.minTextSp], with
 * its line height raised by the same amount.
 */
fun Typography.withTvTextFloor(): Typography = copy(
    displayLarge = displayLarge.floored(), displayMedium = displayMedium.floored(), displaySmall = displaySmall.floored(),
    headlineLarge = headlineLarge.floored(), headlineMedium = headlineMedium.floored(), headlineSmall = headlineSmall.floored(),
    titleLarge = titleLarge.floored(), titleMedium = titleMedium.floored(), titleSmall = titleSmall.floored(),
    bodyLarge = bodyLarge.floored(), bodyMedium = bodyMedium.floored(), bodySmall = bodySmall.floored(),
    labelLarge = labelLarge.floored(), labelMedium = labelMedium.floored(), labelSmall = labelSmall.floored(),
)

/**
 * The TV layout's colours: secondary text halfway between its usual tone and the primary text.
 */
fun ColorScheme.withTvSecondaryContrast(): ColorScheme =
    copy(onSurfaceVariant = lerp(onSurfaceVariant, onSurface, TV_SECONDARY_LIFT))

private fun TextStyle.floored(): TextStyle {
    val floor = ComponentDefaults.TvLayout.minTextSp
    if (fontSize.type != TextUnitType.Sp || fontSize.value >= floor) return this
    val raise = floor - fontSize.value
    return copy(
        fontSize = floor.sp,
        lineHeight = if (lineHeight.type == TextUnitType.Sp) (lineHeight.value + raise).sp else lineHeight
    )
}

private const val TV_SECONDARY_LIFT = 0.5f
