package com.nendo.argosy.data.installer

import com.nendo.argosy.data.local.entity.ManagedInstallerEntity

/**
 * The installer rows this platform seeds. Where [replacesUpstreamSeeds] is true, locked rows that
 * are not in [all] are seeds of the other platform and are removed (rows under [FLATHUB_OWNER]
 * are kept: the platform manages them itself).
 */
expect object InstallerSeeds {
    val all: List<ManagedInstallerEntity>
    val replacesUpstreamSeeds: Boolean
}

const val FLATHUB_OWNER = "flathub"
