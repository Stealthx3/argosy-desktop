package com.nendo.argosy.ui.screens.home.delegates

import com.nendo.argosy.platform.PlatformContext as Context
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
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.className
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.PlatformIntent
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.repository.GameRepository
import com.nendo.argosy.data.remote.romm.RomMResult
import com.nendo.argosy.ui.input.SoundFeedbackManager
import com.nendo.argosy.core.input.SoundType
import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.showError
import com.nendo.argosy.core.notification.showSuccess
import com.nendo.argosy.ui.screens.common.GameActionsDelegate
import com.nendo.argosy.ui.screens.common.RefreshAndroidResult
import com.nendo.argosy.ui.screens.home.HomeGameUi
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MENU_INDEX_MAX_DOWNLOADED = 5
private const val MENU_INDEX_MAX_REMOTE = 4

sealed class GameMenuAction {
    data class Play(val gameId: Long, val needsInstall: Boolean, val isDownloaded: Boolean) : GameMenuAction()
    data class PlayOnDisplay(val gameId: Long, val displayId: Int) : GameMenuAction()
    data class ToggleFavorite(val gameId: Long) : GameMenuAction()
    data class ViewDetails(val gameId: Long) : GameMenuAction()
    data class AddToCollection(val gameId: Long) : GameMenuAction()
    data class ActiveVariant(val gameId: Long) : GameMenuAction()
    data class Refresh(val gameId: Long, val isAndroidApp: Boolean) : GameMenuAction()
    data class ResyncPlatform(val platformId: Long, val platformName: String) : GameMenuAction()
    data class Delete(val gameId: Long) : GameMenuAction()
    data class RemoveFromHome(val gameId: Long) : GameMenuAction()
    data class RemoveFromLibrary(val gameId: Long) : GameMenuAction()
    data class Hide(val gameId: Long) : GameMenuAction()
}

data class GameMenuState(
    val showGameMenu: Boolean = false,
    val gameMenuFocusIndex: Int = 0
)

class HomeGameMenuDelegate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameActions: GameActionsDelegate,
    private val gameRepository: GameRepository,
    private val soundManager: SoundFeedbackManager,
    private val notificationManager: NotificationManager
) {
    private val _state = MutableStateFlow(GameMenuState())
    val state: StateFlow<GameMenuState> = _state.asStateFlow()

    private val _uninstallIntent = MutableSharedFlow<Intent>()
    val uninstallIntent: SharedFlow<Intent> = _uninstallIntent.asSharedFlow()

    fun toggleGameMenu() {
        val wasShowing = _state.value.showGameMenu
        _state.update {
            it.copy(showGameMenu = !it.showGameMenu, gameMenuFocusIndex = 0)
        }
        if (!wasShowing) {
            soundManager.play(SoundType.OPEN_MODAL)
        } else {
            soundManager.play(SoundType.CLOSE_MODAL)
        }
    }

    fun resetMenu() {
        _state.update { it.copy(showGameMenu = false) }
    }

    fun moveGameMenuFocus(
        delta: Int,
        focusedGame: HomeGameUi?,
        isPlatformRow: Boolean,
        extraRows: Int = 0,
        hasSiblingGroup: Boolean = false
    ) {
        _state.update {
            val isDownloaded = focusedGame?.isDownloaded == true
            val needsInstall = focusedGame?.needsInstall == true
            val isRommGame = focusedGame?.isRommGame == true
            val isAndroidApp = focusedGame?.isAndroidApp == true
            val isSyncable = isPlatformRow && focusedGame != null && focusedGame.platformId > 0
            var maxIndex = if (isDownloaded || needsInstall) MENU_INDEX_MAX_DOWNLOADED else MENU_INDEX_MAX_REMOTE
            maxIndex += extraRows
            if (hasSiblingGroup) maxIndex++
            if (isRommGame || isAndroidApp) maxIndex++
            if (isAndroidApp) maxIndex++
            if (isSyncable) maxIndex++
            if (focusedGame?.canRemoveFromLibrary == true) maxIndex++
            val newIndex = (it.gameMenuFocusIndex + delta).coerceIn(0, maxIndex)
            it.copy(gameMenuFocusIndex = newIndex)
        }
    }

    fun resolveMenuAction(
        focusIndex: Int,
        game: HomeGameUi,
        isPlatformRow: Boolean,
        playDisplays: List<Int> = emptyList(),
        hasSiblingGroup: Boolean = false
    ): GameMenuAction {
        var currentIdx = 0
        val playIdx = currentIdx++
        val playOnDisplayRange = if (playDisplays.size > 1 && game.isDownloaded) {
            val start = currentIdx
            currentIdx += playDisplays.size
            start until currentIdx
        } else {
            IntRange.EMPTY
        }
        if (focusIndex in playOnDisplayRange) {
            return GameMenuAction.PlayOnDisplay(
                game.id,
                playDisplays[focusIndex - playOnDisplayRange.first]
            )
        }
        val favoriteIdx = currentIdx++
        val detailsIdx = currentIdx++
        val addToCollectionIdx = currentIdx++
        val activeVariantIdx = if (hasSiblingGroup) currentIdx++ else -1
        val refreshIdx = if (game.isRommGame || game.isAndroidApp) currentIdx++ else -1
        val resyncIdx = if (isPlatformRow && game.platformId > 0) currentIdx++ else -1
        val deleteIdx = if (game.isDownloaded || game.needsInstall) currentIdx++ else -1
        val removeFromHomeIdx = if (game.isAndroidApp) currentIdx++ else -1
        val removeFromLibraryIdx = if (game.canRemoveFromLibrary) currentIdx++ else -1
        val hideIdx = currentIdx

        return when (focusIndex) {
            playIdx -> GameMenuAction.Play(game.id, game.needsInstall, game.isDownloaded)
            favoriteIdx -> GameMenuAction.ToggleFavorite(game.id)
            detailsIdx -> GameMenuAction.ViewDetails(game.id)
            addToCollectionIdx -> GameMenuAction.AddToCollection(game.id)
            activeVariantIdx -> GameMenuAction.ActiveVariant(game.id)
            refreshIdx -> GameMenuAction.Refresh(game.id, game.isAndroidApp)
            resyncIdx -> GameMenuAction.ResyncPlatform(game.platformId, game.platformDisplayName)
            deleteIdx -> GameMenuAction.Delete(game.id)
            removeFromHomeIdx -> GameMenuAction.RemoveFromHome(game.id)
            removeFromLibraryIdx -> GameMenuAction.RemoveFromLibrary(game.id)
            hideIdx -> GameMenuAction.Hide(game.id)
            else -> GameMenuAction.Hide(game.id)
        }
    }

    fun toggleFavorite(scope: CoroutineScope, gameId: Long, onComplete: suspend () -> Unit) {
        scope.launch {
            gameActions.toggleFavorite(gameId)
            onComplete()
        }
    }

    fun hideGame(scope: CoroutineScope, gameId: Long, onComplete: suspend () -> Unit) {
        scope.launch {
            gameActions.hideGame(gameId)
            onComplete()
        }
    }

    fun removeFromHome(scope: CoroutineScope, gameId: Long, onComplete: suspend () -> Unit) {
        scope.launch {
            val game = gameRepository.getById(gameId)
            if (game != null && game.source == GameSource.ANDROID_APP) {
                gameRepository.delete(game)
                onComplete()
                soundManager.play(SoundType.UNFAVORITE)
            }
        }
    }

    fun removeFromLibrary(scope: CoroutineScope, gameId: Long, onComplete: suspend () -> Unit) {
        scope.launch {
            if (gameActions.removeFromLibrary(gameId)) {
                notificationManager.showSuccess(
                    NotificationText.Res(R.string.home_notice_removed_from_library)
                )
                onComplete()
            } else {
                notificationManager.showError(
                    NotificationText.Res(R.string.home_notice_remove_failed)
                )
            }
        }
    }

    fun refreshGameData(scope: CoroutineScope, gameId: Long, onComplete: suspend () -> Unit) {
        scope.launch {
            when (val result = gameActions.refreshGameData(gameId)) {
                is RomMResult.Success -> {
                    notificationManager.showSuccess(
                        NotificationText.Res(R.string.home_notice_game_data_refreshed)
                    )
                    onComplete()
                }
                is RomMResult.Error -> {
                    notificationManager.showError(NotificationText.Raw(result.message))
                }
            }
            toggleGameMenu()
        }
    }

    fun refreshAndroidGameData(scope: CoroutineScope, gameId: Long, onComplete: suspend () -> Unit) {
        scope.launch {
            when (val result = gameActions.refreshAndroidGameData(gameId)) {
                is RefreshAndroidResult.Success -> {
                    notificationManager.showSuccess(
                        NotificationText.Res(R.string.home_notice_android_data_refreshed)
                    )
                    onComplete()
                }
                is RefreshAndroidResult.Error -> {
                    notificationManager.showError(NotificationText.Raw(result.message))
                }
            }
            toggleGameMenu()
        }
    }
}
