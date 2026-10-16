package com.nendo.argosy.data.cache

import com.nendo.argosy.data.model.ArtSlot
import com.nendo.argosy.data.model.ResolvedGameArt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Stub of upstream's 2,352-line ImageCacheManager: the three calls Coil.kt makes, with no cache behind them. */
class ImageCacheManager {
    fun observeArt(gameId: Long): Flow<ResolvedGameArt?> = flowOf(null)
    fun artFor(gameId: Long): ResolvedGameArt? = null
    fun repairMissingArt(gameId: Long, slot: ArtSlot) {}
}
