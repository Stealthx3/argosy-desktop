package com.nendo.argosy.desktop

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

private val RobotoWeights = listOf(
    FontWeight.Thin to "Thin",
    FontWeight.ExtraLight to "ExtraLight",
    FontWeight.Light to "Light",
    FontWeight.Normal to "Regular",
    FontWeight.Medium to "Medium",
    FontWeight.SemiBold to "SemiBold",
    FontWeight.Bold to "Bold",
    FontWeight.ExtraBold to "ExtraBold",
    FontWeight.Black to "Black",
)

/**
 * Android's sans-serif: Roboto at the nine weights fonts.xml names, upright and italic, cut from
 * the platform's variable font by scripts/desktop/fonts/instance-roboto.py and bundled so text
 * never depends on the host's fonts.
 */
val Roboto: FontFamily = FontFamily(
    RobotoWeights.flatMap { (weight, name) ->
        val italic = if (name == "Regular") "Italic" else "${name}Italic"
        listOf(
            Font("fonts/roboto/Roboto-$name.ttf", weight, FontStyle.Normal),
            Font("fonts/roboto/Roboto-$italic.ttf", weight, FontStyle.Italic),
        )
    },
)

fun robotoFontFamilyResolver(): FontFamily.Resolver = RobotoFontResolver.create(Roboto)
