package com.nendo.argosy.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Fake of upstream's Hilt ThemeViewModel: the two flows ArgosyTheme reads, at upstream defaults. */
class ThemeViewModel {
    val themeState: StateFlow<ThemeState> = MutableStateFlow(ThemeState(themeMode = com.nendo.argosy.data.preferences.ThemeMode.DARK))
    val customFonts: StateFlow<CustomFontFamilies> = MutableStateFlow(CustomFontFamilies())
}
