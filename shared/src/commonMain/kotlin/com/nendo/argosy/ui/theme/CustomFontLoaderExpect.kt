package com.nendo.argosy.ui.theme

import androidx.compose.ui.text.font.FontFamily

expect object CustomFontLoader {
    fun loadFamily(path: String): FontFamily?
}
