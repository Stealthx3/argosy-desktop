package com.nendo.argosy.data.sync

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import javax.inject.Inject

actual class RAPatchBackfillWorker @Inject constructor() {
    actual companion object {
        actual fun schedule(context: Context) {
        }
    }
}
