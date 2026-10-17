package com.nendo.argosy.core.locale

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformConfiguration as Configuration
import java.util.Locale

actual object LocaleHelper {
    actual fun wrap(base: Context, languageTag: String): Context = TODO("desktop: wrap")

    actual fun resolveLocale(languageTag: String): Locale? = null

    actual fun overrideConfiguration(base: Configuration?, languageTag: String): Configuration? = null

    actual fun effectiveLocale(context: Context): Locale = TODO("desktop: effectiveLocale")

    actual const val SYSTEM_LANGUAGE_TAG: String = "system"
}
