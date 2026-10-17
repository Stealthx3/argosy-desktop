package com.nendo.argosy.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.nendo.argosy.shared.resources.AndroidResourceIds
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString

actual abstract class PlatformContext {
    abstract val platformResources: PlatformResources
    abstract val platformFilesDir: java.io.File
    abstract val platformCacheDir: java.io.File
}

actual open class PlatformResources {
    open fun text(id: Int): String = runBlocking {
        org.jetbrains.compose.resources.getString(AndroidResourceIds.string(id))
    }

    open fun quantityText(id: Int, quantity: Int): String = runBlocking {
        getPluralString(AndroidResourceIds.plurals(id), quantity)
    }
}

class DesktopPlatformContext(
    override val platformFilesDir: java.io.File = DesktopDirectories.data.resolve("files"),
    override val platformCacheDir: java.io.File = DesktopDirectories.cache,
) : PlatformContext() {
    override val platformResources: PlatformResources = PlatformResources()
}

object DesktopDirectories {
    private fun xdg(variable: String, fallback: String): java.io.File =
        java.io.File(System.getenv(variable)?.takeIf { it.isNotBlank() } ?: (System.getProperty("user.home") + fallback))

    val data: java.io.File get() = xdg("XDG_DATA_HOME", "/.local/share").resolve("argosy-desktop")
    val cache: java.io.File get() = xdg("XDG_CACHE_HOME", "/.cache").resolve("argosy-desktop")
}

actual fun PlatformContext.getString(resId: Int): String = platformResources.text(resId)

actual fun PlatformContext.getString(resId: Int, vararg formatArgs: Any?): String =
    String.format(platformResources.text(resId), *formatArgs)

actual val PlatformContext.resources: PlatformResources get() = platformResources

actual fun PlatformResources.getString(id: Int): String = text(id)

actual fun PlatformResources.getString(id: Int, vararg formatArgs: Any?): String = String.format(text(id), *formatArgs)

actual fun PlatformResources.getQuantityString(id: Int, quantity: Int): String = quantityText(id, quantity)

actual fun PlatformResources.getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any?): String =
    String.format(quantityText(id, quantity), *formatArgs)

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf { DesktopPlatformContext() }

actual val PlatformContext.filesDir: java.io.File get() = platformFilesDir

actual val PlatformContext.cacheDir: java.io.File get() = platformCacheDir
