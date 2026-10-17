package com.nendo.argosy.data.download

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

actual object DownloadNotificationChannel {
    actual fun create(context: Context) {
    }

    actual const val CHANNEL_ID: String = "download_service_channel"
}
