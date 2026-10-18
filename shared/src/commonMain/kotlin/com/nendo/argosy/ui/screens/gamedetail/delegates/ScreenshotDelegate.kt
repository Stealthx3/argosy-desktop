package com.nendo.argosy.ui.screens.gamedetail.delegates

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
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.data.cache.ImageCacheManager
import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.showError
import com.nendo.argosy.core.notification.showSuccess
import com.nendo.argosy.ui.screens.gamedetail.ScreenshotPair
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScreenshotState(
    val focusedScreenshotIndex: Int = 0,
    val showScreenshotViewer: Boolean = false,
    val viewerScreenshotIndex: Int = 0
)

class ScreenshotDelegate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCacheManager: ImageCacheManager,
    private val notificationManager: NotificationManager
) {
    private val _state = MutableStateFlow(ScreenshotState())
    val state: StateFlow<ScreenshotState> = _state.asStateFlow()

    fun reset() {
        _state.value = ScreenshotState()
    }

    fun setFocusedScreenshotIndex(index: Int) {
        _state.update { it.copy(focusedScreenshotIndex = index) }
    }

    fun moveScreenshotFocus(delta: Int, screenshots: List<ScreenshotPair>) {
        if (screenshots.isEmpty()) return
        _state.update { state ->
            val newIndex = (state.focusedScreenshotIndex + delta).mod(screenshots.size)
            state.copy(focusedScreenshotIndex = newIndex)
        }
    }

    fun openScreenshotViewer(screenshots: List<ScreenshotPair>, index: Int? = null) {
        if (screenshots.isEmpty()) return
        val viewerIndex = index ?: _state.value.focusedScreenshotIndex
        _state.update {
            it.copy(
                showScreenshotViewer = true,
                viewerScreenshotIndex = viewerIndex.coerceIn(0, screenshots.size - 1)
            )
        }
    }

    fun closeScreenshotViewer() {
        _state.update { state ->
            state.copy(
                showScreenshotViewer = false,
                focusedScreenshotIndex = state.viewerScreenshotIndex
            )
        }
    }

    fun moveViewerIndex(delta: Int, screenshots: List<ScreenshotPair>) {
        if (screenshots.isEmpty()) return
        _state.update { state ->
            val newIndex = (state.viewerScreenshotIndex + delta).mod(screenshots.size)
            state.copy(viewerScreenshotIndex = newIndex)
        }
    }

    fun setCurrentScreenshotAsBackground(
        scope: CoroutineScope,
        gameId: Long,
        screenshots: List<ScreenshotPair>,
        onSuccess: () -> Unit
    ) {
        val screenshot = screenshots.getOrNull(_state.value.viewerScreenshotIndex) ?: return
        val screenshotPath = screenshot.cachedPath ?: screenshot.remoteUrl

        scope.launch {
            val success = imageCacheManager.setScreenshotAsBackground(gameId, screenshotPath)
            if (success) {
                notificationManager.showSuccess(
                    NotificationText.Res(R.string.gamedetail_notice_background_updated)
                )
                onSuccess()
            } else {
                notificationManager.showError(
                    NotificationText.Res(R.string.gamedetail_notice_background_failed)
                )
            }
        }
    }
}
