package com.nendo.argosy.ui.screens.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.data.model.ActiveSort
import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.model.SortOption
import com.nendo.argosy.data.preferences.GridDensity
import com.nendo.argosy.data.preferences.LibraryLayout
import com.nendo.argosy.data.social.FriendActivity
import com.nendo.argosy.domain.model.SaveListState
import com.nendo.argosy.data.model.SourceFilter
import com.nendo.argosy.domain.model.PlayerCountBucket
import com.nendo.argosy.ui.common.labelRes
import com.nendo.argosy.platform.PlatformContext as Context
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
import com.nendo.argosy.ui.common.labelRes
import com.nendo.argosy.ui.components.ListReorder
import com.nendo.argosy.ui.components.activeFilterSummary
import com.nendo.argosy.ui.screens.common.DiscPickerState
import com.nendo.argosy.ui.screens.common.SyncOverlayState
import com.nendo.argosy.ui.screens.gamedetail.CollectionItemUi
import com.nendo.argosy.ui.screens.home.HomePlatformUi
import com.nendo.argosy.ui.util.GridUtils
import kotlinx.coroutines.flow.map

/**
 * The tabs of the filter modal. Routing runs on the entry itself and on its ordinal, never on the
 * text of the tab, so [labelRes] is only ever the half that shows.
 */
enum class FilterCategory(@StringRes val labelRes: Int) {
    SORT(R.string.library_filter_category_sort),
    SEARCH(R.string.library_filter_category_search),
    SOURCE(R.string.library_filter_category_source),
    PLATFORM(R.string.library_filter_category_platform),
    GENRE(R.string.library_filter_category_genre),
    REGION(R.string.library_filter_category_region),
    PLAYERS(R.string.library_filter_category_players),
    SERIES(R.string.library_filter_category_series)
}

/**
 * One filter category that is on: the tab it belongs to, the label or text that names its value,
 * and how many selections it holds. Every "which filters are active" question reads
 * [ActiveFilters.entries]; nothing else re-derives it from the fields.
 */
data class ActiveFilterEntry(
    val category: FilterCategory,
    @StringRes val labelRes: Int? = null,
    val text: String? = null,
    val count: Int = 1
)

/**
 * A platform a filter is limited to. [id] is what a game is matched against; [label] is carried
 * alongside only so a chip can name it without a lookup, and is never compared.
 */
data class PlatformRef(val id: Long, val label: String)

data class ActiveFilters(
    val searchQuery: String = "",
    val source: SourceFilter = SourceFilter.ALL,
    val platforms: Set<PlatformRef> = emptySet(),
    val genres: Set<String> = emptySet(),
    val regions: Set<String> = emptySet(),
    val players: PlayerCountBucket? = null,
    val series: Set<String> = emptySet(),
    val sort: ActiveSort = ActiveSort()
) {
    val entries: List<ActiveFilterEntry>
        get() = listOfNotNull(
            sort.option.takeIf { it != SortOption.TITLE }
                ?.let { ActiveFilterEntry(FilterCategory.SORT, labelRes = it.labelRes) },
            searchQuery.takeIf { it.isNotEmpty() }
                ?.let { ActiveFilterEntry(FilterCategory.SEARCH, text = it) },
            source.takeIf { it != SourceFilter.ALL }
                ?.let { ActiveFilterEntry(FilterCategory.SOURCE, labelRes = it.labelRes) },
            platforms.firstOrNull()?.let {
                ActiveFilterEntry(FilterCategory.PLATFORM, text = it.label, count = platforms.size)
            },
            multiSelectEntry(FilterCategory.GENRE, genres),
            multiSelectEntry(FilterCategory.REGION, regions),
            players?.let { ActiveFilterEntry(FilterCategory.PLAYERS, labelRes = it.labelRes) },
            multiSelectEntry(FilterCategory.SERIES, series)
        )

    val activeCount: Int
        get() = entries.sumOf { it.count }

    fun isActive(category: FilterCategory): Boolean = entries.any { it.category == category }

    fun summary(context: Context): String =
        chips.activeFilterSummary(context, R.plurals.library_filter_active_count)
            ?: context.getString(R.string.source_filter_all)

    private fun multiSelectEntry(category: FilterCategory, values: Set<String>): ActiveFilterEntry? =
        values.firstOrNull()?.let { ActiveFilterEntry(category, text = it, count = values.size) }
}

data class FilterOptions(
    val platforms: List<PlatformRef> = emptyList(),
    val genres: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    val series: List<String> = emptyList()
)

enum class LibraryFilter(val label: String) {
    ALL("All Games"),
    PLAYABLE("Playable Only"),
    LOCAL("Local Only"),
    SYNCED("Synced Only"),
    REMOTE("Remote Only")
}

enum class FocusMove {
    UP, DOWN, LEFT, RIGHT
}

/**
 * What the library is showing. [PLATFORM_GRID] is the landing when nothing was asked for; arriving
 * with a platform, a source or a configured library default goes straight to [GAMES], because those
 * callers have already chosen and a chooser would be in their way.
 */
enum class LibraryView {
    PLATFORM_GRID, GAMES
}

sealed interface LibraryGridItem {
    data class Header(val label: String, val sidebarLabel: String = label) : LibraryGridItem
    data class Game(val game: LibraryGameUi, val gameIndex: Int) : LibraryGridItem
}

/** Viewport-relative bounds of a focusable cover, used for spatial D-pad navigation in masonry mode. */
data class FocusCellBounds(
    val gameIndex: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class LibraryGameUi(
    val id: Long,
    val title: String,
    val sortTitle: String,
    val platformId: Long,
    val platformSlug: String,
    val platformDisplayName: String,
    val coverPath: String?,
    val boxSpinePath: String? = null,
    val box3dPath: String? = null,
    val gradientColors: Pair<Color, Color>? = null,
    val source: GameSource,
    val isFavorite: Boolean,
    val isDownloaded: Boolean,
    val isRommGame: Boolean,
    val canRemoveFromLibrary: Boolean = false,
    val isAndroidApp: Boolean,
    val emulatorName: String?,
    val needsInstall: Boolean = false,
    val isHidden: Boolean = false,
    val listDetails: com.nendo.argosy.ui.common.GameListDetails
) {
    val sourceIcon: ImageVector?
        get() = when (source) {
            GameSource.LOCAL_ONLY -> Icons.Default.Folder
            GameSource.ROMM_SYNCED -> Icons.Default.CheckCircle
            GameSource.ROMM_REMOTE -> null
            GameSource.STEAM -> Icons.Default.Cloud
            GameSource.ANDROID_APP -> null
            GameSource.GAMENATIVE -> Icons.Default.Cloud
        }
}

data class LibraryUiState(
    val view: LibraryView = LibraryView.PLATFORM_GRID,
    val platformCells: List<LibraryCellUi> = emptyList(),
    val platformGridFocusedIndex: Int = 0,
    val platformReorder: ListReorder<LibraryCellUi>? = null,
    val canReturnToPlatformGrid: Boolean = false,
    val platforms: List<HomePlatformUi> = emptyList(),
    val currentPlatformIndex: Int = -1,
    val games: List<LibraryGameUi> = emptyList(),
    val focusedIndex: Int = 0,
    val lastFocusMove: FocusMove? = null,
    val currentFilter: LibraryFilter = LibraryFilter.ALL,
    val showFilterMenu: Boolean = false,
    val showQuickMenu: Boolean = false,
    val quickMenuFocusIndex: Int = 0,
    val quickMenuHasSiblingGroup: Boolean = false,
    val isCustomGridHome: Boolean = false,
    val gridDensity: GridDensity = GridDensity.NORMAL,
    val libraryLayout: LibraryLayout = LibraryLayout.GRID,
    val boxArt3d: Boolean = false,
    val saveStates: Map<Long, SaveListState> = emptyMap(),
    val friendsActivity: Map<Int, List<FriendActivity>> = emptyMap(),
    val isLoading: Boolean = true,
    val activeFilters: ActiveFilters = ActiveFilters(),
    val filterOptions: FilterOptions = FilterOptions(),
    val filterCategoryIndex: Int = 0,
    val filterOptionIndex: Int = 0,
    val syncOverlayState: SyncOverlayState? = null,
    val discPickerState: DiscPickerState? = null,
    val discPickerFocusIndex: Int = 0,
    val variantPickerState: com.nendo.argosy.ui.screens.common.VariantPickerState? = null,
    val variantPickerFocusIndex: Int = 0,
    val memcardPickerState: com.nendo.argosy.ui.screens.common.MemcardPickerState? = null,
    val memcardPickerFocusIndex: Int = 0,
    val isTouchMode: Boolean = false,
    val hasSelectedGame: Boolean = false,
    val screenWidthDp: Int = 0,
    val recentSearches: List<String> = emptyList(),
    val repairedCoverPaths: Map<Long, String> = emptyMap(),
    val showAddToCollectionModal: Boolean = false,
    val collectionGameId: Long? = null,
    val collections: List<CollectionItemUi> = emptyList(),
    val collectionModalFocusIndex: Int = 0,
    val showCreateCollectionDialog: Boolean = false,
    val gridItems: List<LibraryGridItem> = emptyList(),
    val sectionLabels: List<String> = emptyList(),
    val currentSectionLabel: String = "",
    val showSectionOverlay: Boolean = false,
    val overlaySectionLabel: String = "",
    val sectionJumpTrigger: Int = 0,
    val sectionRailFocusIndex: Int? = null,
    val hiddenGameCount: Int = 0
) {
    val showSectionSidebar: Boolean
        get() = sectionLabels.size >= MIN_SECTION_RAIL_LABELS

    val isSectionRailFocused: Boolean
        get() = sectionRailFocusIndex != null

    val sectionRailFocusedLabel: String?
        get() = sectionRailFocusIndex?.let { sectionLabels.getOrNull(it) }

    val isPlatformGrid: Boolean
        get() = view == LibraryView.PLATFORM_GRID

    val isReorderingPlatforms: Boolean
        get() = platformReorder != null

    val focusedPlatformCell: LibraryCellUi?
        get() = platformCells.getOrNull(platformGridFocusedIndex)?.takeIf { it.isPlatform }

    /**
     * A platform cell is a badge - a mark and two short lines - so it wants the game grid's narrower
     * columns rather than the app grid's wide ones. The landing only earns its place while the whole
     * shelf is in front of you, and app-width columns spent that on air.
     */
    val platformGridColumns: Int
        get() = GridUtils.getGameGridColumns(gridDensity, screenWidthDp)

    /**
     * Nothing to land on: no platform row, no media library, and nothing in the library as a whole.
     * A user with games but no platform rows still gets the grid, since All Games alone is a real
     * destination and hiding it would strand them, and so does one whose only collection is a media
     * library.
     */
    val platformGridIsEmpty: Boolean
        get() = platformCells.none { !it.isAllGames } &&
            platformCells.sumOf { it.itemCount } == 0

    val platformCellCount: Int
        get() = platformCells.count { it.isPlatform }

    val mediaCellCount: Int
        get() = platformCells.count { it.isMedia }

    val isListLayout: Boolean
        get() = libraryLayout == LibraryLayout.LIST

    val columnsCount: Int
        get() = if (isListLayout) 1 else GridUtils.getGameGridColumns(gridDensity, screenWidthDp)

    val gridSpacingDp: Int
        get() = GridUtils.getGridSpacingDp(gridDensity)

    val currentPlatform: HomePlatformUi?
        get() = if (currentPlatformIndex >= 0) platforms.getOrNull(currentPlatformIndex) else null

    val focusedGame: LibraryGameUi?
        get() = games.getOrNull(focusedIndex)

    val currentFilterCategory: FilterCategory
        get() = FilterCategory.entries.getOrElse(filterCategoryIndex) { FilterCategory.SOURCE }

    fun currentCategoryOptions(context: Context): List<String> =
        when (currentFilterCategory) {
            FilterCategory.SORT -> SortOption.entries.map { option ->
                val directionIndicator = if (option == activeFilters.sort.option) {
                    if (activeFilters.sort.descending) " v" else " ^"
                } else ""
                context.getString(option.labelRes) + directionIndicator
            }
            FilterCategory.SEARCH -> recentSearches
            FilterCategory.SOURCE -> SourceFilter.entries.map { filter ->
                val label = context.getString(filter.labelRes)
                if (filter == SourceFilter.HIDDEN && hiddenGameCount > 0)
                    "$label ($hiddenGameCount)"
                else label
            }
            FilterCategory.PLATFORM -> filterOptions.platforms.map { it.label }
            FilterCategory.GENRE -> filterOptions.genres
            FilterCategory.REGION -> filterOptions.regions
            FilterCategory.PLAYERS -> PlayerCountBucket.entries.map { context.getString(it.labelRes) }
            FilterCategory.SERIES -> filterOptions.series
        }

    val isCurrentCategoryMultiSelect: Boolean
        get() = currentFilterCategory !in listOf(
            FilterCategory.SORT,
            FilterCategory.SOURCE,
            FilterCategory.SEARCH,
            FilterCategory.PLAYERS
        )

    val selectedSourceIndex: Int
        get() = activeFilters.source.ordinal

    val selectedSortIndex: Int
        get() = activeFilters.sort.option.ordinal

    val selectedPlayersIndex: Int
        get() = activeFilters.players?.ordinal ?: -1

    val selectedIndicesInCurrentCategory: Set<Int>
        get() = when (currentFilterCategory) {
            FilterCategory.PLATFORM -> {
                val ids = activeFilters.platforms.map { it.id }.toSet()
                filterOptions.platforms.indices.filter { filterOptions.platforms[it].id in ids }.toSet()
            }
            FilterCategory.GENRE -> indicesOf(filterOptions.genres, activeFilters.genres)
            FilterCategory.REGION -> indicesOf(filterOptions.regions, activeFilters.regions)
            FilterCategory.SERIES -> indicesOf(filterOptions.series, activeFilters.series)
            FilterCategory.SORT,
            FilterCategory.SEARCH,
            FilterCategory.SOURCE,
            FilterCategory.PLAYERS -> emptySet()
        }

    private fun indicesOf(options: List<String>, selected: Set<String>): Set<Int> =
        options.indices.filter { options[it] in selected }.toSet()

    val availableCategories: List<FilterCategory>
        get() = FilterCategory.entries.filter { category ->
            when (category) {
                FilterCategory.SORT -> true
                FilterCategory.SEARCH -> true
                FilterCategory.SOURCE -> true
                FilterCategory.PLATFORM -> filterOptions.platforms.isNotEmpty()
                FilterCategory.GENRE -> filterOptions.genres.isNotEmpty()
                FilterCategory.REGION -> filterOptions.regions.isNotEmpty()
                FilterCategory.PLAYERS -> true
                FilterCategory.SERIES -> filterOptions.series.isNotEmpty()
            }
        }
}

internal const val MIN_SECTION_RAIL_LABELS = 3
