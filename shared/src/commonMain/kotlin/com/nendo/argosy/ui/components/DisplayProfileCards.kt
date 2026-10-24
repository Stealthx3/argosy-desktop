package com.nendo.argosy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nendo.argosy.data.preferences.DisplayProfile
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.ui.common.detailRes
import com.nendo.argosy.ui.common.labelRes
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.util.clickableNoFocus

/**
 * The three display profiles as cards, each with a schematic of the Home screen on that kind of
 * screen at the profile's starting scale and safe area. Used by the first-run Display setup step
 * and by Settings, Interface.
 */
@Composable
fun DisplayProfileCards(
    selected: DisplayProfile?,
    focused: DisplayProfile?,
    recommended: DisplayProfile?,
    onChoose: (DisplayProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)) {
        DisplayProfile.entries.forEach { profile ->
            DisplayProfileCard(
                profile = profile,
                isSelected = profile == selected,
                isFocused = profile == focused,
                isRecommended = profile == recommended,
                onClick = { onChoose(profile) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DisplayProfileCard(
    profile: DisplayProfile,
    isSelected: Boolean,
    isFocused: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Dimens.radiusMd)
    val borderColor = when {
        isFocused -> colors.primary
        isSelected -> colors.primary.copy(alpha = 0.5f)
        else -> colors.outlineVariant
    }
    Column(
        modifier = modifier
            .background(if (isFocused) colors.primaryContainer else colors.surfaceVariant, shape)
            .border(if (isFocused) Dimens.borderMedium else Dimens.borderThin, borderColor, shape)
            .clickableNoFocus(onClick)
            .padding(Dimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
    ) {
        val screen = colors.surface
        val element = colors.onSurface.copy(alpha = 0.35f)
        val accent = colors.primary
        val frame = colors.onSurfaceVariant
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(SKETCH_ASPECT)) {
            drawProfileSketch(profile, screen, element, accent, frame)
        }
        Text(
            text = stringResource(profile.labelRes),
            style = MaterialTheme.typography.titleSmall,
            color = if (isFocused) colors.onPrimaryContainer else colors.onSurface
        )
        Text(
            text = stringResource(profile.detailRes),
            style = MaterialTheme.typography.bodySmall,
            color = if (isFocused) colors.onPrimaryContainer else colors.onSurfaceVariant
        )
        if (isRecommended) {
            Text(
                text = stringResource(R.string.settings_interface_display_profile_recommended),
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary
            )
        }
    }
}

private fun DrawScope.drawProfileSketch(
    profile: DisplayProfile,
    screen: Color,
    element: Color,
    accent: Color,
    frame: Color
) {
    val w = size.width
    val h = size.height
    val line = w * FRAME_STROKE
    val screenRect = when (profile) {
        DisplayProfile.HANDHELD -> {
            val body = Rect(w * 0.02f, h * 0.16f, w * 0.98f, h * 0.84f)
            drawRoundRect(frame, body.topLeft, body.size, CornerRadius(h * 0.2f), style = Stroke(line))
            drawCircle(frame, h * 0.07f, Offset(w * 0.1f, h * 0.5f), style = Stroke(line))
            drawCircle(frame, h * 0.035f, Offset(w * 0.9f, h * 0.42f))
            drawCircle(frame, h * 0.035f, Offset(w * 0.86f, h * 0.56f))
            Rect(w * 0.2f, h * 0.22f, w * 0.8f, h * 0.78f)
        }
        DisplayProfile.DESKTOP -> {
            drawLine(frame, Offset(w * 0.5f, h * 0.8f), Offset(w * 0.5f, h * 0.92f), line)
            drawLine(frame, Offset(w * 0.38f, h * 0.94f), Offset(w * 0.62f, h * 0.94f), line)
            Rect(w * 0.08f, h * 0.04f, w * 0.92f, h * 0.8f)
        }
        DisplayProfile.TV -> {
            drawLine(frame, Offset(w * 0.18f, h * 0.88f), Offset(w * 0.14f, h * 0.97f), line)
            drawLine(frame, Offset(w * 0.82f, h * 0.88f), Offset(w * 0.86f, h * 0.97f), line)
            Rect(w * 0.02f, h * 0.04f, w * 0.98f, h * 0.88f)
        }
    }
    drawRect(screen, screenRect.topLeft, screenRect.size)
    drawRect(frame, screenRect.topLeft, screenRect.size, style = Stroke(line))
    val margin = profile.defaultSafeAreaPercent / 100f
    val inner = Rect(
        screenRect.left + screenRect.width * margin,
        screenRect.top + screenRect.height * margin,
        screenRect.right - screenRect.width * margin,
        screenRect.bottom - screenRect.height * margin
    )
    drawHomeSketch(inner, profile.defaultUiScale / 100f, element, accent)
}

private fun DrawScope.drawHomeSketch(area: Rect, scale: Float, element: Color, accent: Color) {
    val aw = area.width
    val ah = area.height
    val corner = CornerRadius(ah * 0.03f)
    drawRoundRect(element, Offset(area.left + aw * 0.04f, area.top + ah * 0.05f), Size(aw * 0.42f * scale, ah * 0.08f * scale), CornerRadius(ah * 0.04f))
    val heroSize = Size(aw * 0.2f * scale, ah * 0.52f * scale)
    val heroTop = Offset(area.left + aw * 0.04f, area.top + ah * 0.22f)
    drawRoundRect(accent, heroTop, heroSize, corner)
    val cardSize = Size(aw * 0.09f * scale, ah * 0.24f * scale)
    val gap = aw * 0.02f
    var x = heroTop.x + heroSize.width + gap
    val y = heroTop.y + heroSize.height - cardSize.height
    while (x + cardSize.width <= area.right - aw * 0.02f) {
        drawRoundRect(element, Offset(x, y), cardSize, corner)
        x += cardSize.width + gap
    }
    val footerHeight = ah * 0.08f * scale
    drawRect(element.copy(alpha = element.alpha * 0.6f), Offset(area.left, area.bottom - footerHeight), Size(aw, footerHeight))
}

private const val SKETCH_ASPECT = 16f / 10f
private const val FRAME_STROKE = 0.012f
