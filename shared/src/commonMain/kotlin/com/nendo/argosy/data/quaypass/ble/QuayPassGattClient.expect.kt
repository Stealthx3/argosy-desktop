package com.nendo.argosy.data.quaypass.ble

import com.nendo.argosy.platform.PlatformApplication as Application
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.resources

expect class QuayPassGattClient(application: Application) {
    companion object {}
}
