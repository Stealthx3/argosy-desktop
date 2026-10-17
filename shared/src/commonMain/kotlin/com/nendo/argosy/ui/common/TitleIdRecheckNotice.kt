package com.nendo.argosy.ui.common

import com.nendo.argosy.platform.PlatformContext as Context
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
import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.showError
import com.nendo.argosy.core.notification.showSuccess
import com.nendo.argosy.data.emulator.TitleIdRecheck

fun NotificationManager.reportTitleIdRecheck(context: Context, result: TitleIdRecheck) {
    when (result) {
        is TitleIdRecheck.Found -> showSuccess(
            if (result.replaced != null) {
                NotificationText.Res(R.string.title_id_recheck_notice_updated, listOf(result.titleId))
            } else {
                NotificationText.Res(R.string.title_id_recheck_notice_confirmed, listOf(result.titleId))
            }
        )
        is TitleIdRecheck.NotFound -> showError(
            NotificationText.Res(R.string.title_id_recheck_notice_not_found, listOf(result.fileName))
        )
        TitleIdRecheck.NoFile -> showError(NotificationText.Res(R.string.title_id_recheck_notice_no_file))
        TitleIdRecheck.Unsupported -> Unit
    }
}
