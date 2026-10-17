package com.nendo.argosy.data.platform

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class InstalledAppResolver @Inject constructor() {
    actual fun getLaunchableApps(): List<LaunchableApp> = emptyList()

    actual fun isAppInstalled(packageName: String): Boolean = false
}
