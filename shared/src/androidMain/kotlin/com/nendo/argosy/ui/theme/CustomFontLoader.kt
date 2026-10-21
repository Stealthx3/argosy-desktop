package com.nendo.argosy.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.FontFamily
import java.io.File

actual object CustomFontLoader {

    actual fun fontsDir(context: Context): File = File(context.filesDir, "fonts")

    actual fun loadFamily(path: String): FontFamily? {
        val file = File(path)
        if (!file.isFile) return null
        return try {
            validate(file)
            FontFamily(typefaceWithSystemFallback(file))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * A user-supplied font covers the scripts its designer drew and nothing else, so a family
     * built from it alone renders tofu for every other script. Chaining the system fallback
     * behind it keeps the chosen face for the glyphs it has and borrows the rest.
     */
    private fun typefaceWithSystemFallback(file: File): Typeface {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Typeface.createFromFile(file)
        return try {
            val font = android.graphics.fonts.Font.Builder(file).build()
            val family = android.graphics.fonts.FontFamily.Builder(font).build()
            Typeface.CustomFallbackBuilder(family)
                .setSystemFallback("sans-serif")
                .build()
        } catch (e: Exception) {
            Typeface.createFromFile(file)
        }
    }

    /** Throws if the file is not a parseable font. */
    actual fun validate(file: File) {
        val typeface = Typeface.createFromFile(file)
        check(typeface != null && typeface != Typeface.DEFAULT) { "Typeface could not be loaded" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.graphics.fonts.Font.Builder(file).build()
        }
    }
}
