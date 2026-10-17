package com.nendo.argosy.data.iconpack

import com.nendo.argosy.data.model.IconPack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class IconPackRepository @Inject constructor() {
    actual suspend fun installedPacks(): List<IconPack> = emptyList()
}
