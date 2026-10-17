package com.nendo.argosy.core.locale

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformConfiguration as Configuration
import java.util.Locale

expect object LocaleHelper {
    val SYSTEM_LANGUAGE_TAG: String

    fun wrap(base: Context, languageTag: String): Context

    fun resolveLocale(languageTag: String): Locale?

    fun overrideConfiguration(base: Configuration?, languageTag: String): Configuration?

    fun effectiveLocale(context: Context): Locale
}
