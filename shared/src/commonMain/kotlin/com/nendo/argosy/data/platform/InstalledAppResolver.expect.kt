package com.nendo.argosy.data.platform

expect class InstalledAppResolver {
    fun getLaunchableApps(): List<LaunchableApp>

    fun isAppInstalled(packageName: String): Boolean
}
