package com.nendo.argosy.ui.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.local.entity.getDisplayName
import com.nendo.argosy.data.remote.romm.RomMRepository
import com.nendo.argosy.data.repository.DownloadFileStatusRepository
import com.nendo.argosy.data.repository.GameRepository
import com.nendo.argosy.data.repository.PlatformRepository
import com.nendo.argosy.data.social.FriendActivity
import com.nendo.argosy.domain.model.PresentationLayout
import com.nendo.argosy.ui.dualscreen.CompanionDetail
import com.nendo.argosy.ui.dualscreen.PresentationSlot
import com.nendo.argosy.ui.screens.home.toCompanionDetail
import com.nendo.argosy.DualScreenManagerHolder
import com.nendo.argosy.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val COLLECTION_SHOWCASE_COVERS = 24

/**
 * What the presentation screen shows for a focused game or a set of games, built the same way
 * whichever screen has the focus, so a game or collection looks identical there from Home, the
 * Library or a collection.
 */
@Singleton
class PresentationShowcaseSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameRepository: GameRepository,
    private val platformRepository: PlatformRepository,
    private val downloadStatus: DownloadFileStatusRepository,
    private val romMRepository: RomMRepository
) {

    suspend fun gameDetail(
        gameId: Long,
        friendsByIgdbId: Map<Int, List<FriendActivity>> = emptyMap()
    ): CompanionDetail? {
        val game = gameRepository.getById(gameId) ?: return null
        return describe(game, friendsByIgdbId)
    }

    /**
     * Fetches the focused game's logo when the presentation layout draws one and the game has
     * none yet. True when a logo arrived, so the caller republishes the detail.
     */
    suspend fun backfillLogo(gameId: Long): Boolean {
        val layout = DualScreenManagerHolder.instance?.presentationStyle?.value?.layout
        if (layout != PresentationLayout.LOGO) return false
        val game = gameRepository.getById(gameId) ?: return false
        if (gameRepository.getArt(gameId).logoPath != null || game.rommId == null) return false
        return romMRepository.fetchLogo(gameId)?.startsWith("/") == true
    }

    suspend fun collectionShowcase(name: String, gameIds: List<Long>): PresentationSlot.PlatformShowcase =
        gameShowcase(
            context = context,
            name = name,
            coverPaths = gameRepository.coverPathsForGames(gameIds, COLLECTION_SHOWCASE_COVERS),
            stats = gameRepository.statsForGames(gameIds),
            fallbackCount = gameIds.size
        )

    private suspend fun describe(
        game: GameEntity,
        friendsByIgdbId: Map<Int, List<FriendActivity>>
    ): CompanionDetail {
        val platformName = platformRepository.getById(game.platformId)?.getDisplayName()
        val friends = game.igdbId?.let { friendsByIgdbId[it.toInt()] }.orEmpty()
        return game.toHomeGameUi(
            downloadStatus,
            gameRepository.getArt(game.id),
            gameRepository.getScreenshots(game.id).firstOrNull()?.sourceUrl,
            platformDisplayName = platformName
        ).toCompanionDetail(friends)
    }
}
