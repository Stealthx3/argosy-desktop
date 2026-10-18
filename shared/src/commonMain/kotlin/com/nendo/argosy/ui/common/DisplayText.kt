package com.nendo.argosy.ui.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.nendo.argosy.res.pluralStringResource
import com.nendo.argosy.res.stringResource

/**
 * Localizable text computed outside a composable (a ViewModel state property or similar).
 * [Raw] is reserved for content the app did not author itself - a file name, a device model,
 * a track title - never for a sentence composed in English here. [Res] and [Plural] carry a
 * string resource id plus format args, resolved in the UI layer via [resolve].
 */
sealed interface DisplayText {
    data class Raw(val text: String) : DisplayText
    data class Res(@StringRes val resId: Int, val args: List<Any> = emptyList()) : DisplayText
    data class Plural(@PluralsRes val resId: Int, val count: Int, val args: List<Any> = emptyList()) : DisplayText
}

@Composable
fun DisplayText.resolve(): String = when (this) {
    is DisplayText.Raw -> text
    is DisplayText.Res -> stringResource(resId, *args.toTypedArray())
    is DisplayText.Plural -> pluralStringResource(resId, count, *args.toTypedArray())
}

fun DisplayText.resolve(context: Context): String = when (this) {
    is DisplayText.Raw -> text
    is DisplayText.Res -> context.getString(resId, *args.toTypedArray())
    is DisplayText.Plural -> context.resources.getQuantityString(resId, count, *args.toTypedArray())
}
