package com.nendo.argosy.ui.components

import com.nendo.argosy.di.desktopComponent
import com.nendo.argosy.platform.PlatformContext
import com.nendo.argosy.platform.applicationContext

actual fun launchProgressEntryPoint(context: PlatformContext): LaunchProgressEntryPoint =
    context.applicationContext.desktopComponent
