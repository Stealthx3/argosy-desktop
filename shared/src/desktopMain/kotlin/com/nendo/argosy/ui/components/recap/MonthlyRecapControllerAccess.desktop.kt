package com.nendo.argosy.ui.components.recap

import com.nendo.argosy.di.desktopComponent
import com.nendo.argosy.platform.PlatformContext
import com.nendo.argosy.platform.applicationContext

actual fun monthlyRecapController(context: PlatformContext): MonthlyRecapController =
    context.applicationContext.desktopComponent.monthlyRecapController()
