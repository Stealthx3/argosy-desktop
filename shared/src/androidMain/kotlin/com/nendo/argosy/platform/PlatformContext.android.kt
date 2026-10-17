package com.nendo.argosy.platform

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.platform.LocalContext

actual typealias PlatformContext = Context

actual typealias PlatformResources = Resources

actual fun PlatformContext.getString(resId: Int): String = getString(resId)

actual fun PlatformContext.getString(resId: Int, vararg formatArgs: Any?): String = getString(resId, *formatArgs)

actual val PlatformContext.resources: PlatformResources get() = resources

actual fun PlatformResources.getString(id: Int): String = getString(id)

actual fun PlatformResources.getString(id: Int, vararg formatArgs: Any?): String = getString(id, *formatArgs)

actual fun PlatformResources.getQuantityString(id: Int, quantity: Int): String = getQuantityString(id, quantity)

actual fun PlatformResources.getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any?): String =
    getQuantityString(id, quantity, *formatArgs)

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> = LocalContext

actual val PlatformContext.filesDir: java.io.File get() = filesDir

actual val PlatformContext.cacheDir: java.io.File get() = cacheDir

actual typealias PlatformAssets = android.content.res.AssetManager

actual val PlatformContext.assets: PlatformAssets get() = assets

actual fun PlatformAssets.list(path: String): Array<String>? = list(path)

actual fun PlatformAssets.open(fileName: String): java.io.InputStream = open(fileName)
