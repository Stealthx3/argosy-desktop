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
