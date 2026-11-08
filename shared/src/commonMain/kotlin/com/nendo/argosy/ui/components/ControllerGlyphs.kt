package com.nendo.argosy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.nendo.argosy.core.input.AndroidKeyCodes as KeyEvent
import com.nendo.argosy.ui.input.LocalButtonGlyphRemap
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.generated.ColorTokens

enum class ControllerFamily { XBOX, PLAYSTATION, NINTENDO, STEAM }

fun controllerFamily(guid: String?, nintendoLayout: Boolean): ControllerFamily {
    val vendor = guid?.takeIf { it.length >= VENDOR_END }?.substring(VENDOR_START, VENDOR_END)?.lowercase()
    return when (vendor) {
        SONY_VENDOR_LE -> ControllerFamily.PLAYSTATION
        NINTENDO_VENDOR_LE -> ControllerFamily.NINTENDO
        VALVE_VENDOR_LE -> ControllerFamily.STEAM
        else -> if (nintendoLayout) ControllerFamily.NINTENDO else ControllerFamily.XBOX
    }
}

fun controllerFamily(layout: String, guid: String?, nintendoLayout: Boolean): ControllerFamily = when (layout) {
    LAYOUT_XBOX -> ControllerFamily.XBOX
    LAYOUT_PLAYSTATION -> ControllerFamily.PLAYSTATION
    LAYOUT_NINTENDO -> ControllerFamily.NINTENDO
    LAYOUT_STEAM -> ControllerFamily.STEAM
    else -> controllerFamily(guid, nintendoLayout)
}

@Composable
fun currentControllerFamily(): ControllerFamily {
    val remap = LocalButtonGlyphRemap.current
    return controllerFamily(remap.layout, remap.controller, remap.nintendo)
}

val showsControllerGlyphs: Boolean
    @Composable get() = com.nendo.argosy.ui.util.LocalDisplayProfile.current != null

/**
 * Draws the connected controller's own mark for the physical [keyCode]: a coloured letter or
 * shape for a face button, a labelled chip for a bumper or trigger. Returns false and draws
 * nothing for any other key.
 */
@Composable
fun ControllerButtonGlyph(keyCode: Int, size: Dp, fontSize: TextUnit, modifier: Modifier = Modifier): Boolean {
    val family = currentControllerFamily()
    val face = faceMark(keyCode, family)
    if (face != null) {
        Box(
            modifier = modifier.size(size).background(face.fill, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (face.shape != null) {
                PlayStationShape(face.shape, face.ink, size * SHAPE_SCALE)
            } else {
                Text(text = face.letter, color = face.ink, style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold))
            }
        }
        return true
    }
    val shoulder = shoulderLabel(keyCode, family) ?: return false
    Box(
        modifier = modifier
            .sizeIn(minWidth = size, minHeight = size)
            .background(ColorTokens.Domain.ControllerGlyph.neutral, RoundedCornerShape(Dimens.radiusSm))
            .padding(horizontal = Dimens.spacingXs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shoulder,
            color = ColorTokens.Domain.ControllerGlyph.onGlyph,
            style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold)
        )
    }
    return true
}

private enum class PsShape { CROSS, CIRCLE, SQUARE, TRIANGLE }

private class FaceMark(val letter: String, val shape: PsShape?, val fill: Color, val ink: Color)

private fun faceMark(keyCode: Int, family: ControllerFamily): FaceMark? {
    val glyph = ColorTokens.Domain.ControllerGlyph
    val on = glyph.onGlyph
    return when (family) {
        ControllerFamily.XBOX -> when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> FaceMark(LETTER_A, null, glyph.xboxA, on)
            KeyEvent.KEYCODE_BUTTON_B -> FaceMark(LETTER_B, null, glyph.xboxB, on)
            KeyEvent.KEYCODE_BUTTON_X -> FaceMark(LETTER_X, null, glyph.xboxX, on)
            KeyEvent.KEYCODE_BUTTON_Y -> FaceMark(LETTER_Y, null, glyph.xboxY, glyph.onLight)
            else -> null
        }
        ControllerFamily.PLAYSTATION -> when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> FaceMark(LETTER_A, PsShape.CROSS, glyph.neutral, glyph.psCross)
            KeyEvent.KEYCODE_BUTTON_B -> FaceMark(LETTER_B, PsShape.CIRCLE, glyph.neutral, glyph.psCircle)
            KeyEvent.KEYCODE_BUTTON_X -> FaceMark(LETTER_X, PsShape.SQUARE, glyph.neutral, glyph.psSquare)
            KeyEvent.KEYCODE_BUTTON_Y -> FaceMark(LETTER_Y, PsShape.TRIANGLE, glyph.neutral, glyph.psTriangle)
            else -> null
        }
        ControllerFamily.NINTENDO -> when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> FaceMark(LETTER_B, null, glyph.neutral, on)
            KeyEvent.KEYCODE_BUTTON_B -> FaceMark(LETTER_A, null, glyph.neutral, on)
            KeyEvent.KEYCODE_BUTTON_X -> FaceMark(LETTER_Y, null, glyph.neutral, on)
            KeyEvent.KEYCODE_BUTTON_Y -> FaceMark(LETTER_X, null, glyph.neutral, on)
            else -> null
        }
        ControllerFamily.STEAM -> when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> FaceMark(LETTER_A, null, glyph.neutral, on)
            KeyEvent.KEYCODE_BUTTON_B -> FaceMark(LETTER_B, null, glyph.neutral, on)
            KeyEvent.KEYCODE_BUTTON_X -> FaceMark(LETTER_X, null, glyph.neutral, on)
            KeyEvent.KEYCODE_BUTTON_Y -> FaceMark(LETTER_Y, null, glyph.neutral, on)
            else -> null
        }
    }
}

private fun shoulderLabel(keyCode: Int, family: ControllerFamily): String? {
    val names = when (family) {
        ControllerFamily.XBOX -> XBOX_SHOULDERS
        ControllerFamily.PLAYSTATION, ControllerFamily.STEAM -> PS_SHOULDERS
        ControllerFamily.NINTENDO -> NINTENDO_SHOULDERS
    }
    return when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_L1 -> names[0]
        KeyEvent.KEYCODE_BUTTON_R1 -> names[1]
        KeyEvent.KEYCODE_BUTTON_L2 -> names[2]
        KeyEvent.KEYCODE_BUTTON_R2 -> names[3]
        else -> null
    }
}

@Composable
private fun PlayStationShape(shape: PsShape, color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = Stroke(width = this.size.minDimension * STROKE_FRACTION)
        val w = this.size.width
        val h = this.size.height
        when (shape) {
            PsShape.CROSS -> {
                drawLine(color, Offset(0f, 0f), Offset(w, h), stroke.width)
                drawLine(color, Offset(w, 0f), Offset(0f, h), stroke.width)
            }
            PsShape.CIRCLE -> drawCircle(color, radius = w / 2f - stroke.width / 2f, style = stroke)
            PsShape.SQUARE -> drawRect(
                color,
                topLeft = Offset(stroke.width / 2f, stroke.width / 2f),
                size = Size(w - stroke.width, h - stroke.width),
                style = stroke
            )
            PsShape.TRIANGLE -> drawPath(
                Path().apply {
                    moveTo(w / 2f, stroke.width / 2f)
                    lineTo(w - stroke.width / 2f, h - stroke.width / 2f)
                    lineTo(stroke.width / 2f, h - stroke.width / 2f)
                    close()
                },
                color,
                style = stroke
            )
        }
    }
}

private const val LAYOUT_XBOX = "xbox"
private const val LAYOUT_PLAYSTATION = "playstation"
private const val LAYOUT_NINTENDO = "nintendo"
private const val LAYOUT_STEAM = "steam"
private const val VENDOR_START = 8
private const val VENDOR_END = 12
private const val SONY_VENDOR_LE = "4c05"
private const val NINTENDO_VENDOR_LE = "7e05"
private const val VALVE_VENDOR_LE = "de28"
private const val SHAPE_SCALE = 0.5f
private const val STROKE_FRACTION = 0.16f
private const val LETTER_A = "A"
private const val LETTER_B = "B"
private const val LETTER_X = "X"
private const val LETTER_Y = "Y"
private val XBOX_SHOULDERS = listOf("LB", "RB", "LT", "RT")
private val PS_SHOULDERS = listOf("L1", "R1", "L2", "R2")
private val NINTENDO_SHOULDERS = listOf("L", "R", "ZL", "ZR")

@Composable
fun ShoulderGlyph(keyCode: Int, painter: androidx.compose.ui.graphics.painter.Painter, tint: Color, size: Dp, contentDescription: String?) {
    if (showsControllerGlyphs &&
        ControllerButtonGlyph(keyCode, size, androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize)
    ) {
        return
    }
    androidx.compose.material3.Icon(
        painter = painter,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}
