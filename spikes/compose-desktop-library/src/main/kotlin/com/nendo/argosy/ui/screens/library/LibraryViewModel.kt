package com.nendo.argosy.ui.screens.library

import com.nendo.argosy.ui.screens.home.GameDownloadIndicator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fake of upstream's 2,568-line Hilt LibraryViewModel: the members LibraryGameList calls, with the
 * same bodies upstream has minus sounds, sticky-column state and game-navigation context.
 * State is seeded by the spike (see spike.FakeLibrary).
 */
class LibraryViewModel {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val downloadIndicators: StateFlow<Map<Long, GameDownloadIndicator>> = MutableStateFlow(emptyMap())

    fun seed(state: LibraryUiState) { _uiState.value = state }

    fun gameIndexToGridIndex(gameIndex: Int): Int {
        val gridItems = _uiState.value.gridItems
        for ((gridIdx, item) in gridItems.withIndex()) {
            if (item is LibraryGridItem.Game && item.gameIndex == gameIndex) return gridIdx
        }
        return 0
    }

    fun enterTouchMode() {
        _uiState.update { it.copy(isTouchMode = true, hasSelectedGame = false, sectionRailFocusIndex = null) }
    }

    fun repairCoverImage(gameId: Long, failedPath: String) {}

    fun handleItemTap(index: Int, onGameSelect: (Long) -> Unit) {
        val state = _uiState.value
        if (index < 0 || index >= state.games.size) return
        if (!state.hasSelectedGame || index != state.focusedIndex) {
            _uiState.update {
                it.copy(focusedIndex = index, hasSelectedGame = true, isTouchMode = true, sectionRailFocusIndex = null)
            }
            return
        }
        onGameSelect(state.games[index].id)
    }

    fun handleItemLongPress(index: Int) {
        val state = _uiState.value
        if (index < 0 || index >= state.games.size) return
        if (index != state.focusedIndex) {
            _uiState.update { it.copy(focusedIndex = index, hasSelectedGame = true, isTouchMode = true) }
        }
    }
}
