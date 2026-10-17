package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent

actual object StorageAccess {
    actual fun isGranted(context: Context): Boolean = false

    actual fun isGranted(sdkInt: Int, isExternalStorageManager: () -> Boolean, isPermissionGranted: (String) -> Boolean): Boolean = false

    actual fun settingsIntent(context: Context): Intent = TODO("desktop: settingsIntent")
}
