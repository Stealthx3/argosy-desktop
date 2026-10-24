package com.nendo.argosy.data.installer

import com.nendo.argosy.data.emulator.LinuxInstallKind
import com.nendo.argosy.data.local.entity.ManagedInstallerEntity
import com.nendo.argosy.data.emulator.EmulatorRegistry
import com.nendo.argosy.data.emulator.LinuxEmulators

actual object InstallerSeeds {
    actual val all: List<ManagedInstallerEntity>
        get() = LinuxEmulators.all.mapNotNull { entry ->
            val repo = entry.appImageRepo ?: return@mapNotNull null
            ManagedInstallerEntity(
                repoOwner = repo.substringBefore('/'),
                repoName = repo.substringAfter('/'),
                displayName = entry.displayName,
                packageName = EmulatorRegistry.getById(entry.id)?.packageName?.let { InstallToken.of(LinuxInstallKind.APPIMAGE, it) },
                locked = true,
            )
        }.distinctBy { it.repoOwner to it.repoName }.mapIndexed { index, seed -> seed.copy(sortOrder = index) }

    actual val replacesUpstreamSeeds: Boolean = true
}
