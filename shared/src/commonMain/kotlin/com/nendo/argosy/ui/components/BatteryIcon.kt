package com.nendo.argosy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.nendo.argosy.ui.theme.generated.ColorTokens
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal const val BATTERY_LOW_PERCENT = 20

@Composable
internal fun BatteryIcon(
    level: Int,
    isCharging: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fillColor = when {
        isCharging -> ColorTokens.Domain.Battery.charging
        level <= BATTERY_LOW_PERCENT -> ColorTokens.Domain.Battery.low
        else -> color
    }

    Canvas(modifier = modifier) {
        val bodyWidth = size.width - 4.dp.toPx()
        val bodyHeight = size.height
        val cornerRadius = 2.dp.toPx()
        val strokeWidth = 1.5f.dp.toPx()
        val padding = strokeWidth
        val terminalWidth = ComponentDefaults.BatteryIndicator.terminalWidthDp.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth)
        )

        drawRect(
            color = color,
            topLeft = Offset(bodyWidth, bodyHeight * 0.3f),
            size = Size(terminalWidth, bodyHeight * 0.4f)
        )

        val fillWidth = ((bodyWidth - padding * 2) * (level / 100f)).coerceAtLeast(0f)
        if (fillWidth > 0) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(padding, padding),
                size = Size(fillWidth, bodyHeight - padding * 2),
                cornerRadius = CornerRadius(cornerRadius / 2, cornerRadius / 2)
            )
        }

        if (isCharging) {
            val centerX = bodyWidth / 2
            val centerY = bodyHeight / 2
            val boltSize = bodyHeight * 0.6f

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(centerX + boltSize * 0.1f, centerY - boltSize * 0.4f)
                lineTo(centerX - boltSize * 0.2f, centerY)
                lineTo(centerX + boltSize * 0.05f, centerY)
                lineTo(centerX - boltSize * 0.1f, centerY + boltSize * 0.4f)
                lineTo(centerX + boltSize * 0.2f, centerY)
                lineTo(centerX - boltSize * 0.05f, centerY)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}
