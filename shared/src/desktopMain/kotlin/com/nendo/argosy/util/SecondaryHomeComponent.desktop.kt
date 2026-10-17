package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

actual object SecondaryHomeComponent {
    actual fun setEnabled(context: Context, enabled: Boolean) {
    }

    actual fun isDefaultHome(context: Context): Boolean = false
}
