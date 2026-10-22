package com.nendo.argosy.ui.components.recap

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.data.repository.MIN_DISPLAY_MS
import com.nendo.argosy.util.formatPlayTime
import java.time.Duration

object RecapPlayTime {
    /**
     * [activeMs] in letter units, with any positive time under [MIN_DISPLAY_MS] read as under a
     * minute.
     */
    fun of(context: Context, activeMs: Long): String =
        if (activeMs in 1 until MIN_DISPLAY_MS) {
            context.getString(R.string.ui_monthly_recap_playtime_under_minute)
        } else {
            formatPlayTime(context, Duration.ofMillis(activeMs).toMinutes().toInt())
        }
}
