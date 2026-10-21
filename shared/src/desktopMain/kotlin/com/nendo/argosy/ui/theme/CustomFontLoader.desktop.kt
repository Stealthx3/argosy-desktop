package com.nendo.argosy.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.nendo.argosy.platform.PlatformContext
import com.nendo.argosy.platform.filesDir
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import java.io.File

actual object CustomFontLoader {
    actual fun fontsDir(context: PlatformContext): File = File(context.filesDir, "fonts")

    actual fun loadFamily(path: String): FontFamily? {
        val file = File(path)
        if (!file.isFile) return null
        return runCatching { FontFamily(Font(file)) }.getOrNull()
    }

    actual fun validate(file: File) {
        val typeface = FontMgr.default.makeFromData(Data.makeFromBytes(file.readBytes()))
        check(typeface != null) { "Typeface could not be loaded" }
    }
}
