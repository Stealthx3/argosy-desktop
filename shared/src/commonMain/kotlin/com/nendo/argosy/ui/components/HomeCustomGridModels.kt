package com.nendo.argosy.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.nendo.argosy.res.ArgosyR as R

/**
 * What a tile shows once resolved. The grid does not know how to load a cover, so the host supplies
 * one of these per tile and an unresolvable target is simply absent from the map.
 */
data class CustomGridTileContent(
    val game: com.nendo.argosy.ui.screens.home.HomeGameUi?,
    val label: String,
    val media: com.nendo.argosy.ui.screens.home.HomeMediaUi? = null,
    val isMissing: Boolean = false,
    val packageName: String? = null,
    val coverPath: String? = null,
    val posterUrl: String? = null,
    val subtitle: String? = null,
    val stats: List<TileStat> = emptyList(),
    /**
     * Marks a tile that plays one game out of a collection, so a queue is distinguishable from the
     * plain game tile it otherwise looks exactly like.
     */
    val isCollectionQueue: Boolean = false,
    /**
     * Marks a random game tile, which otherwise looks exactly like the game it happens to be
     * showing. The die badge and the re-roll animation hang off this.
     */
    val isRandom: Boolean = false,
    /**
     * Marks the continue tile, which likewise looks exactly like the game it last showed.
     */
    val isContinue: Boolean = false,
    val isLibraryLink: Boolean = false,
    /**
     * What a RetroAchievements tile draws, in place of every other field here. Present only on
     * that tile, which owns its own layout in each cell shape rather than borrowing one.
     */
    val ra: RaTileUi? = null
)

/**
 * One fact worth reading at a glance on a tile with room for it. Kept as a label and value pair so
 * the tile decides how many fit rather than each caller guessing.
 */
data class TileStat(val label: String, val value: String)

/**
 * A collection a tile points at, resolved for drawing: the name it shows and one cover from inside
 * it, since a collection has no art of its own.
 */
data class TileCollectionUi(
    val name: String,
    val coverPaths: List<String> = emptyList(),
    val gameCount: Int = 0
) {
    val coverPath: String? get() = coverPaths.firstOrNull()
}

/**
 * What a wide game tile says beside its cover. Only facts that exist are offered: a game with no
 * achievements and no play time shows nothing rather than a row of zeroes, which reads as broken.
 */
fun tileStatsFor(game: com.nendo.argosy.ui.screens.home.HomeGameUi, context: Context): List<TileStat> = buildList {
    if (game.playTimeMinutes > 0) {
        add(TileStat(context.getString(R.string.home_grid_tile_stat_played), formatPlayTime(context, game.playTimeMinutes)))
    }
    if (game.achievementCount > 0) {
        add(
            TileStat(
                context.getString(R.string.home_grid_tile_stat_achievements),
                "${game.earnedAchievementCount}/${game.achievementCount}"
            )
        )
    }
    if (game.releaseYear != null) {
        add(TileStat(context.getString(R.string.home_grid_tile_stat_released), game.releaseYear.toString()))
    }
}
