package com.nendo.argosy.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.nendo.argosy.shared.resources.AndroidResourceIds
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString

actual abstract class PlatformContext {
    abstract val platformResources: PlatformResources
}

actual open class PlatformResources {
    open fun text(id: Int): String = runBlocking {
        org.jetbrains.compose.resources.getString(AndroidResourceIds.string(id))
    }

    open fun quantityText(id: Int, quantity: Int): String = runBlocking {
        getPluralString(AndroidResourceIds.plurals(id), quantity)
    }
}

class DesktopPlatformContext : PlatformContext() {
    override val platformResources: PlatformResources = PlatformResources()
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
