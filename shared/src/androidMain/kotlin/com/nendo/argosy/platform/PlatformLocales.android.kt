package com.nendo.argosy.platform

import android.app.LocaleManager
import android.os.LocaleList
import androidx.annotation.RequiresApi

@RequiresApi(33)
actual fun setApplicationLocales(context: PlatformContext, tag: String?) {
    val localeManager = context.getSystemService(LocaleManager::class.java)
    localeManager?.applicationLocales = if (tag == null) {
        LocaleList.getEmptyLocaleList()
    } else {
        LocaleList.forLanguageTags(tag)
    }
}
