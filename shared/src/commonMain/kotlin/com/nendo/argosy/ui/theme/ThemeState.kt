package com.nendo.argosy.ui.theme

import com.nendo.argosy.data.cache.GradientPreset
import com.nendo.argosy.data.preferences.BoxArtBorderStyle
import com.nendo.argosy.data.preferences.BoxArtBorderThickness
import com.nendo.argosy.data.preferences.BoxArtCornerRadius
import com.nendo.argosy.data.preferences.BoxArtShape
import com.nendo.argosy.data.preferences.BoxArtGlowStrength
import com.nendo.argosy.data.preferences.BoxArtInnerEffect
import com.nendo.argosy.data.preferences.BoxArtInnerEffectThickness
import com.nendo.argosy.data.preferences.BoxArtOuterEffect
import com.nendo.argosy.data.preferences.BoxArtOuterEffectThickness
import com.nendo.argosy.data.preferences.GlowColorMode
import com.nendo.argosy.data.preferences.GripReserveMode
import com.nendo.argosy.data.preferences.PlatformIndicatorContent
import com.nendo.argosy.data.preferences.PlatformIndicatorStyle
import com.nendo.argosy.data.preferences.SystemIconPadding
import com.nendo.argosy.data.preferences.SystemIconPosition
import com.nendo.argosy.data.preferences.ThemeMode
import com.nendo.argosy.ui.theme.backdrop.BackdropConfig

data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val primaryColor: Int? = null,
    val secondaryColor: Int? = null,
    val tertiaryColor: Int? = null,
    val surfaceTintBleed: Int = 0,
    val surfaceBackdrop: BackdropConfig = BackdropConfig(),
    val boxArtShape: BoxArtShape = BoxArtShape.STANDARD,
    val boxArtCornerRadius: BoxArtCornerRadius = BoxArtCornerRadius.MEDIUM,
    val boxArtBorderThickness: BoxArtBorderThickness = BoxArtBorderThickness.MEDIUM,
    val boxArtBorderStyle: BoxArtBorderStyle = BoxArtBorderStyle.SOLID,
    val glassBorderTintAlpha: Float = 0f,
    val boxArtGlowStrength: BoxArtGlowStrength = BoxArtGlowStrength.MEDIUM,
    val boxArtOuterEffect: BoxArtOuterEffect = BoxArtOuterEffect.GLOW,
    val boxArtOuterEffectThickness: BoxArtOuterEffectThickness = BoxArtOuterEffectThickness.MEDIUM,
    val glowColorMode: GlowColorMode = GlowColorMode.AUTO,
    val boxArtInnerEffect: BoxArtInnerEffect = BoxArtInnerEffect.SHADOW,
    val boxArtInnerEffectThickness: BoxArtInnerEffectThickness = BoxArtInnerEffectThickness.MEDIUM,
    val gradientPreset: GradientPreset = GradientPreset.BALANCED,
    val systemIconPosition: SystemIconPosition = SystemIconPosition.TOP_LEFT,
    val systemIconPadding: SystemIconPadding = SystemIconPadding.MEDIUM,
    val platformIndicatorStyle: PlatformIndicatorStyle = PlatformIndicatorStyle.TAB,
    val platformIndicatorContent: PlatformIndicatorContent = PlatformIndicatorContent.NAME,
    val useAccentColorFooter: Boolean = false,
    val compactFooter: Boolean = false,
    val uiScale: Int = 100,
    val gripReserveMode: GripReserveMode = GripReserveMode.OFF,
    val gripReservePercent: Int = GRIP_RESERVE_DEFAULT_PERCENT,
    val gripAutoControllerConnected: Boolean = false,
    val displayFontScale: Int = 100,
    val bodyFontScale: Int = 100
)
