package com.nendo.argosy.hardware

import androidx.annotation.StringRes
import com.nendo.argosy.res.ArgosyR as R

enum class CompanionPanel(@StringRes val labelRes: Int) {
    DASHBOARD(R.string.dual_companion_panel_dashboard),
    ACHIEVEMENTS(R.string.dual_companion_panel_achievements)
}

data class CompanionInGameState(
    val gameId: Long = -1,
    val title: String = "",
    val coverPath: String? = null,
    val platformName: String = "",
    val developer: String? = null,
    val releaseYear: Int? = null,
    val playTimeMinutes: Int = 0,
    val playCount: Int = 0,
    val achievementCount: Int = 0,
    val earnedAchievementCount: Int = 0,
    val sessionStartTimeMillis: Long = 0,
    val channelName: String? = null,
    val isHardcore: Boolean = false,
    val isDirty: Boolean = false,
    val isLoaded: Boolean = false,
    val quickActionsAvailable: Boolean = false,
    val backgroundPath: String? = null,
    val manual: com.nendo.argosy.ui.screens.gamedetail.GameDocument? = null,
    val walkthrough: com.nendo.argosy.ui.screens.gamedetail.GameDocument? = null,
    val manualLastPage: Int? = null,
    val walkthroughProgress: Float? = null
)

/**
 * Applies live quick-action state after asynchronously loaded game metadata arrives.
 * The flag can change while the metadata is being loaded, so the metadata snapshot
 * must not replace it with its default.
 */
internal fun CompanionInGameState.withLiveQuickActionState(
    quickActionsAvailable: Boolean
): CompanionInGameState = copy(quickActionsAvailable = quickActionsAvailable)
