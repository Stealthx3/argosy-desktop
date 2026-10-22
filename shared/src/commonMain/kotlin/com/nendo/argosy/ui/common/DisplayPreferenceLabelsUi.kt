package com.nendo.argosy.ui.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
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
import androidx.annotation.StringRes
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.data.cache.GradientPreset
import com.nendo.argosy.data.preferences.BoxArtShape

/**
 * The display label. `GradientPreset` lives in `data/cache` and must not import `R`, so the label
 * is attached here beside the persisted enum name.
 */
@get:StringRes
val GradientPreset.labelRes: Int
    get() = when (this) {
        GradientPreset.VIBRANT -> R.string.gradient_preset_vibrant
        GradientPreset.BALANCED -> R.string.gradient_preset_balanced
        GradientPreset.SUBTLE -> R.string.gradient_preset_subtle
        GradientPreset.CUSTOM -> R.string.gradient_preset_custom
    }

/**
 * The picker label. Every value but [BoxArtShape.NATIVE] is an aspect ratio ("2:3", "3:4", "1:1")
 * and must never be translated; only the word naming the non-ratio choice is.
 */
fun BoxArtShape.label(context: Context): String = when (this) {
    BoxArtShape.NATIVE -> context.getString(R.string.box_art_shape_native)
    else -> displayName
}
