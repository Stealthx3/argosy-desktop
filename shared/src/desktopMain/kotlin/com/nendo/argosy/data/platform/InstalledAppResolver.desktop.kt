package com.nendo.argosy.data.platform

import com.nendo.argosy.data.apps.HostApplications
import com.nendo.argosy.data.repository.AppsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class InstalledAppResolver @Inject constructor(
    private val appsRepository: AppsRepository
) {
    actual fun getLaunchableApps(): List<LaunchableApp> =
        HostApplications.all()
            .asSequence()
            .filter { !appsRepository.isArgosy(it.id) }
            .map { LaunchableApp(packageName = it.id, displayName = it.name) }
            .sortedBy { it.displayName.lowercase() }
            .toList()

    actual fun isAppInstalled(packageName: String): Boolean = HostApplications.find(packageName) != null
}
