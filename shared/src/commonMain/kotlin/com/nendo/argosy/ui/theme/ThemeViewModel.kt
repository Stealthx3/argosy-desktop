package com.nendo.argosy.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nendo.argosy.config.AppBuildConfig as BuildConfig
import com.nendo.argosy.data.preferences.GripReserveMode
import androidx.compose.ui.text.font.FontFamily
import com.nendo.argosy.data.preferences.FontSlot
import com.nendo.argosy.data.preferences.ThemeMode
import com.nendo.argosy.data.preferences.UserPreferences
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.hardware.AmbientLedManager
import com.nendo.argosy.ui.theme.backdrop.BackdropConfig
import com.nendo.argosy.di.HiltViewModel
import kotlinx.coroutines.Dispatchers
import com.nendo.argosy.core.input.ConnectedControllerTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nendo.argosy.platform.PlatformColorUtils as ColorUtils
import javax.inject.Inject

private const val LED_HUE_SATURATION = 0.7f
private const val LED_HUE_LIGHTNESS = 0.5f

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val ambientLedManager: AmbientLedManager,
    private val connectedControllerTracker: ConnectedControllerTracker
) : ViewModel() {

    init {
        observeThemeForLed()
    }

    private fun observeThemeForLed() {
        preferencesRepository.userPreferences
            .onEach { prefs ->
                if (prefs.ambientLedCustomColor) {
                    val custom = Color(
                        ColorUtils.HSLToColor(
                            floatArrayOf(
                                prefs.ambientLedCustomColorHue.toFloat(),
                                LED_HUE_SATURATION,
                                LED_HUE_LIGHTNESS
                            )
                        )
                    )
                    ambientLedManager.setUiColors(custom, null)
                } else {
                    val defaultPrimary = if (BuildConfig.DEBUG) ALauncherColors.Orange else ALauncherColors.Indigo
                    val primary = prefs.primaryColor?.let { Color(it) } ?: defaultPrimary
                    val secondary = prefs.secondaryColor?.let { Color(it) }
                    ambientLedManager.setUiColors(primary, secondary)
                }
            }
            .launchIn(viewModelScope)
    }

    val themeState: StateFlow<ThemeState> = combine(
        preferencesRepository.userPreferences,
        connectedControllerTracker.connectedControllerIds
    ) { prefs, connectedIds ->
        prefs.toThemeState().copy(
            gripAutoControllerConnected = isGripAutoControllerConnected(
                autoEnabled = prefs.gripReserveMode == GripReserveMode.AUTO,
                autoControllerIds = prefs.gripAutoControllers.controllerIds,
                connectedControllerIds = connectedIds
            )
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeState()
        )

    private val fontFamilyCache = mutableMapOf<String, FontFamily>()

    val customFonts: StateFlow<CustomFontFamilies> = preferencesRepository.userPreferences
        .map { it.displayFontPath to it.bodyFontPath }
        .distinctUntilChanged()
        .map { (displayPath, bodyPath) ->
            withContext(Dispatchers.IO) {
                CustomFontFamilies(
                    display = resolveFontFamily(FontSlot.DISPLAY, displayPath),
                    body = resolveFontFamily(FontSlot.BODY, bodyPath)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CustomFontFamilies()
        )

    private suspend fun resolveFontFamily(slot: FontSlot, path: String?): FontFamily? {
        if (path == null) return null
        fontFamilyCache[path]?.let { return it }
        val family = CustomFontLoader.loadFamily(path)
        if (family == null) {
            preferencesRepository.setCustomFont(slot, null, null)
        } else {
            fontFamilyCache[path] = family
        }
        return family
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setCustomColors(primary: Int?, secondary: Int?, tertiary: Int?) {
        viewModelScope.launch {
            preferencesRepository.setCustomColors(primary, secondary, tertiary)
        }
    }

    fun setUiScale(scale: Int) {
        viewModelScope.launch {
            preferencesRepository.setUiScale(scale)
        }
    }
}

/**
 * Pure mapping from persisted [UserPreferences] to [ThemeState]. Lives here so
 * the secondary-display Activity (not a Hilt entry point, can't use
 * hiltViewModel()) can build its own ThemeState flow from
 * preferencesRepository.userPreferences without instantiating ThemeViewModel
 * a second time -- doing that would spin up a duplicate ambient-LED observer.
 */
fun UserPreferences.toThemeState(): ThemeState = ThemeState(
    themeMode = themeMode,
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
    tertiaryColor = tertiaryColor,
    surfaceTintBleed = surfaceTintBleed,
    surfaceBackdrop = BackdropConfig(
        enabled = backdropEnabled,
        preset = backdropPreset,
        cellSize = backdropCellSize,
        scatter = backdropScatter,
        scaleJitter = backdropScaleJitter,
        strength = backdropStrength,
        edgeStyle = backdropEdgeStyle,
        vertexIcons = backdropVertexIcons,
        seed = backdropSeed,
        motion = backdropMotion,
        motionSpeed = backdropMotionSpeed,
        driftAngle = backdropDriftAngle
    ),
    boxArtShape = boxArtShape,
    boxArtCornerRadius = boxArtCornerRadius,
    boxArtBorderThickness = boxArtBorderThickness,
    boxArtBorderStyle = boxArtBorderStyle,
    glassBorderTintAlpha = glassBorderTint.alpha,
    boxArtGlowStrength = boxArtGlowStrength,
    boxArtOuterEffect = boxArtOuterEffect,
    boxArtOuterEffectThickness = boxArtOuterEffectThickness,
    glowColorMode = glowColorMode,
    boxArtInnerEffect = boxArtInnerEffect,
    boxArtInnerEffectThickness = boxArtInnerEffectThickness,
    gradientPreset = gradientPreset,
    systemIconPosition = systemIconPosition,
    systemIconPadding = systemIconPadding,
    platformIndicatorStyle = platformIndicatorStyle,
    platformIndicatorContent = platformIndicatorContent,
    useAccentColorFooter = useAccentColorFooter,
    compactFooter = compactFooter,
    uiScale = uiScale,
    gripReserveMode = gripReserveMode,
    gripReservePercent = gripReservePercent,
    displayFontScale = displayFontScale,
    bodyFontScale = bodyFontScale
)
