package com.nendo.argosy.data.installer

import com.nendo.argosy.data.local.entity.ManagedInstallerEntity

actual object InstallerSeeds {
    actual val all: List<ManagedInstallerEntity> get() = SeededInstallers.ALL
    actual val replacesUpstreamSeeds: Boolean = false
}
