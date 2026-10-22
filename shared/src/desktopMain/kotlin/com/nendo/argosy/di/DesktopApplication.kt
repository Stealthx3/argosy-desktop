package com.nendo.argosy.di

import com.nendo.argosy.platform.DesktopDirectories
import com.nendo.argosy.platform.PlatformApplication
import com.nendo.argosy.platform.PlatformContext

class DesktopApplication : PlatformApplication() {

    val component: DesktopComponent by lazy {
        DaggerDesktopComponent.factory().create(DesktopDirectories.data, this)
    }
}

val PlatformContext.desktopComponent: DesktopComponent
    get() = (this as? DesktopApplication)?.component
        ?: error("desktop graph is reached through the DesktopApplication context")
