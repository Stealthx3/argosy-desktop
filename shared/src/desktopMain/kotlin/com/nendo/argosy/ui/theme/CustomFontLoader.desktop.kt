package com.nendo.argosy.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import java.io.File

actual object CustomFontLoader {
    actual fun loadFamily(path: String): FontFamily? {
        val file = File(path)
        if (!file.isFile) return null
        return runCatching { FontFamily(Font(file)) }.getOrNull()
    }
}
