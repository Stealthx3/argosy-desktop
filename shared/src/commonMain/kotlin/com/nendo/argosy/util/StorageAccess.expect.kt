package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent

expect object StorageAccess {
    fun isGranted(context: Context): Boolean

    fun settingsIntent(context: Context): Intent

    fun isGranted(sdkInt: Int, isExternalStorageManager: () -> Boolean, isPermissionGranted: (String) -> Boolean): Boolean
}
