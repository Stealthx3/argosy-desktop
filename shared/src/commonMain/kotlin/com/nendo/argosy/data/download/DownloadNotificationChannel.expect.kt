package com.nendo.argosy.data.download

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

expect object DownloadNotificationChannel {
    val CHANNEL_ID: String

    fun create(context: Context)
}
