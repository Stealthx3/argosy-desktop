package com.nendo.argosy.data.sync

import com.nendo.argosy.di.desktopComponent
import com.nendo.argosy.platform.PlatformContext as Context

actual class RAPatchBackfillWorker {
    actual companion object {
        actual fun schedule(context: Context) {
            context.desktopComponent.backgroundJobs().scheduleRAPatchBackfill()
        }
    }
}
