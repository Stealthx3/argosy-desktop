package com.nendo.argosy.platform

import androidx.compose.runtime.ProvidableCompositionLocal

expect abstract class PlatformContext

expect open class PlatformResources

expect fun PlatformContext.getString(resId: Int): String

expect fun PlatformContext.getString(resId: Int, vararg formatArgs: Any?): String

expect val PlatformContext.resources: PlatformResources

expect fun PlatformResources.getString(id: Int): String

expect fun PlatformResources.getString(id: Int, vararg formatArgs: Any?): String

expect fun PlatformResources.getQuantityString(id: Int, quantity: Int): String

expect fun PlatformResources.getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any?): String

expect val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext>

expect val PlatformContext.filesDir: java.io.File

expect val PlatformContext.cacheDir: java.io.File

expect class PlatformAssets

expect val PlatformContext.assets: PlatformAssets

expect fun PlatformAssets.list(path: String): Array<String>?

expect fun PlatformAssets.open(fileName: String): java.io.InputStream
