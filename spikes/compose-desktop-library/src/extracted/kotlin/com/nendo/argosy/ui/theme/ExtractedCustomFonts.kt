// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/ui/theme/CustomFonts.kt: CustomFontFamilies withSlot argosyTypography
package com.nendo.argosy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.nendo.argosy.ui.theme.generated.TypographyTokens

data class CustomFontFamilies(
    val display: FontFamily? = null,
    val body: FontFamily? = null
)

private fun TextStyle.withSlot(family: FontFamily?, scale: Int): TextStyle {
    val styled = if (family != null) copy(fontFamily = family) else this
    if (scale == 100) return styled
    val factor = scale / 100f
    return styled.copy(fontSize = styled.fontSize * factor, lineHeight = styled.lineHeight * factor)
}

/** Material typography with DISPLAY driving display/headline/title styles and BODY driving body/label styles; per-slot scale (percent) multiplies fontSize and lineHeight; defaults pass through untouched. */
fun argosyTypography(fonts: CustomFontFamilies, displayScale: Int = 100, bodyScale: Int = 100): Typography {
    if (fonts.display == null && fonts.body == null && displayScale == 100 && bodyScale == 100) {
        return TypographyTokens.Material3
    }
    return Typography(
        displayLarge = TypographyTokens.displayLarge.withSlot(fonts.display, displayScale),
        displayMedium = TypographyTokens.displayMedium.withSlot(fonts.display, displayScale),
        displaySmall = TypographyTokens.displaySmall.withSlot(fonts.display, displayScale),
        headlineLarge = TypographyTokens.headlineLarge.withSlot(fonts.display, displayScale),
        headlineMedium = TypographyTokens.headlineMedium.withSlot(fonts.display, displayScale),
        headlineSmall = TypographyTokens.headlineSmall.withSlot(fonts.display, displayScale),
        titleLarge = TypographyTokens.titleLarge.withSlot(fonts.display, displayScale),
        titleMedium = TypographyTokens.titleMedium.withSlot(fonts.display, displayScale),
        titleSmall = TypographyTokens.titleSmall.withSlot(fonts.display, displayScale),
        bodyLarge = TypographyTokens.bodyLarge.withSlot(fonts.body, bodyScale),
        bodyMedium = TypographyTokens.bodyMedium.withSlot(fonts.body, bodyScale),
        bodySmall = TypographyTokens.bodySmall.withSlot(fonts.body, bodyScale),
        labelLarge = TypographyTokens.labelLarge.withSlot(fonts.body, bodyScale),
        labelMedium = TypographyTokens.labelMedium.withSlot(fonts.body, bodyScale),
        labelSmall = TypographyTokens.labelSmall.withSlot(fonts.body, bodyScale)
    )
}
