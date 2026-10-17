package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformConfiguration as Configuration

expect object DesktopMode {
    fun isSupported(context: Context): Boolean

    fun isActive(context: Context): Boolean

    fun isActive(configuration: Configuration): Boolean
}
