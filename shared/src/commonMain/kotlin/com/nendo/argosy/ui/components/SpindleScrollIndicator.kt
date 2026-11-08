package com.nendo.argosy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.nendo.argosy.ui.theme.LocalActiveGamePalette
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.theme.generated.ComponentDefaults

/**
 * A thin vertical scroll indicator for [state]: a tapered thumb with pointed ends over a faint
 * track, with a soft accent glow behind the thumb. Draws nothing while the list fits.
 */
@Composable
fun SpindleScrollIndicator(state: LazyListState, modifier: Modifier = Modifier) {
    val accent = LocalActiveGamePalette.current.firstOrNull() ?: LocalArgosyTheme.current.focusAccent
    val track = LocalArgosyTheme.current.focusAccent.copy(alpha = TRACK_ALPHA)
    val thumb by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val total = info.totalItemsCount
            val visible = info.visibleItemsInfo
            if (total == 0 || visible.isEmpty() || (!state.canScrollForward && !state.canScrollBackward)) {
                null
            } else {
                val first = visible.first()
                val firstFraction = if (first.size > 0) {
                    (info.viewportStartOffset - first.offset).toFloat() / first.size
                } else {
                    0f
                }
                val shown = visible.size.toFloat().coerceAtMost(total.toFloat())
                val start = ((first.index + firstFraction) / total).coerceIn(0f, 1f)
                val length = (shown / total).coerceIn(MIN_THUMB_FRACTION, 1f)
                start.coerceAtMost(1f - length) to length
            }
        }
    }
    val glowWidth = ComponentDefaults.TvLayout.scrollbarWidthDp.dp * GLOW_WIDTH_FACTOR
    Canvas(modifier = modifier.fillMaxHeight().width(glowWidth)) {
        val current = thumb ?: return@Canvas
        val width = ComponentDefaults.TvLayout.scrollbarWidthDp.dp.toPx()
        val centerX = size.width / 2f
        drawLine(track, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = width / 3f)
        val top = current.first * size.height
        val bottom = top + current.second * size.height
        drawSpindle(centerX, top, bottom, size.width, accent.copy(alpha = ComponentDefaults.TvLayout.scrollbarGlowAlpha * GLOW_OUTER))
        drawSpindle(centerX, top, bottom, width * 2f, accent.copy(alpha = ComponentDefaults.TvLayout.scrollbarGlowAlpha))
        drawSpindle(centerX, top, bottom, width, accent)
    }
}

private fun DrawScope.drawSpindle(centerX: Float, top: Float, bottom: Float, width: Float, color: Color) {
    val half = width / 2f
    val taper = ((bottom - top) * TAPER_FRACTION).coerceAtMost(width * TAPER_MAX_WIDTHS)
    val path = Path().apply {
        moveTo(centerX, top)
        lineTo(centerX + half, top + taper)
        lineTo(centerX + half, bottom - taper)
        lineTo(centerX, bottom)
        lineTo(centerX - half, bottom - taper)
        lineTo(centerX - half, top + taper)
        close()
    }
    drawPath(path, color)
}

private const val TRACK_ALPHA = 0.12f
private const val MIN_THUMB_FRACTION = 0.12f
private const val GLOW_WIDTH_FACTOR = 4
private const val GLOW_OUTER = 0.4f
private const val TAPER_FRACTION = 0.25f
private const val TAPER_MAX_WIDTHS = 6f
