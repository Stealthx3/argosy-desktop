package com.nendo.argosy.data.iconpack

import com.nendo.argosy.data.model.IconPack

expect class IconPackRepository {
    suspend fun installedPacks(): List<IconPack>
}
