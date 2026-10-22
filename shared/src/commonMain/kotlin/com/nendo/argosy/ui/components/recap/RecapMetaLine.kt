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
import com.nendo.argosy.domain.model.RecapLastSession
import com.nendo.argosy.util.formatMonthDay
import com.nendo.argosy.util.formatPlayTime
import com.nendo.argosy.util.formatRelativeTime
import java.time.Duration
import java.time.Instant

object RecapMetaLine {
    /**
     * The pre-launch recap's line about [session]: when it ended relative to [now], its length when
     * at least [MIN_DISPLAY_MS], and [unlockCount] when given and positive.
     */
    fun of(
        context: Context,
        session: RecapLastSession,
        now: Instant,
        unlockCount: Int? = null,
        absolute: (Instant) -> String = ::formatMonthDay
    ): String {
        val relative = formatRelativeTime(context, session.end, now, absolute)
        val line = if (session.activePlayMs >= MIN_DISPLAY_MS) {
            context.getString(
                R.string.ui_launch_recap_meta_duration,
                relative,
                formatPlayTime(context, Duration.ofMillis(session.activePlayMs).toMinutes().toInt())
            )
        } else {
            context.getString(R.string.ui_launch_recap_meta, relative)
        }
        if (unlockCount == null || unlockCount <= 0) return line
        return context.resources.getQuantityString(R.plurals.ui_launch_recap_meta_unlocked, unlockCount, line, unlockCount)
    }
}
