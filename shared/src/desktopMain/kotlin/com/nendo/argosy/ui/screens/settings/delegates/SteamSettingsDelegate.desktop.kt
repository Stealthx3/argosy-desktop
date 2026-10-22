package com.nendo.argosy.ui.screens.settings.delegates

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.data.launcher.GameNativeSyncFolder
import com.nendo.argosy.data.steam.SteamService
import com.nendo.argosy.ui.screens.settings.SteamSettingsState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

actual class SteamSettingsDelegate @Inject constructor() {
    actual val state: StateFlow<SteamSettingsState> = kotlinx.coroutines.flow.MutableStateFlow(SteamSettingsState())

    actual val requestStoragePermissionEvent: SharedFlow<Unit> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual val openUrlEvent: SharedFlow<String> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual val openStoreSyncDirPicker: SharedFlow<GameNativeSyncFolder> = kotlinx.coroutines.flow.MutableSharedFlow()

    actual fun moveGameNativeActionFocus(delta: Int): Boolean = false

    actual fun openGameNativeFoldersModal() {
    }

    actual fun dismissGameNativeFoldersModal() {
    }

    actual fun moveGameNativeFoldersFocus(delta: Int) {
    }

    actual fun moveGameNativeFoldersActionFocus(delta: Int): Boolean = false

    actual fun confirmGameNativeFoldersRow(scope: CoroutineScope) {
    }

    actual fun openStoreSyncDirPicker(scope: CoroutineScope, folder: GameNativeSyncFolder) {
    }

    actual fun setStoreSyncDir(scope: CoroutineScope, folder: GameNativeSyncFolder, path: String) {
    }

    actual fun clearStoreSyncDir(scope: CoroutineScope, folder: GameNativeSyncFolder) {
    }

    actual fun rescanStoreSync(scope: CoroutineScope) {
    }

    actual fun updateState(newState: SteamSettingsState) {
    }

    actual fun bindService(service: SteamService, scope: CoroutineScope) {
    }

    actual fun loadSteamSettings(context: Context, scope: CoroutineScope) {
    }

    actual fun cycleSteamInstallVolume(scope: CoroutineScope, direction: Int) {
    }

    actual fun installSteamLauncher(emulatorId: String, scope: CoroutineScope) {
    }

    actual fun connectToSteam(context: Context, scope: CoroutineScope) {
    }

    actual fun unbindService(context: Context) {
    }

    actual fun startQrAuth() {
    }

    actual fun cancelQrAuth() {
    }

    actual fun syncLibrary(context: Context, scope: CoroutineScope) {
    }

    actual fun forceSyncLibraryWithOverwrite(context: Context, scope: CoroutineScope) {
    }

    actual fun disconnectSteam(context: Context, scope: CoroutineScope) {
    }

    actual fun resetLibrary(scope: CoroutineScope) {
    }

    actual fun showAddSteamGameDialog() {
    }

    actual fun dismissAddSteamGameDialog() {
    }

    actual fun setAddGameAppId(appId: String) {
    }

    actual fun confirmAddSteamGame(context: Context, scope: CoroutineScope, launcherPackage: String) {
    }

    actual val downloadProgress: kotlinx.coroutines.flow.Flow<com.nendo.argosy.data.emulator.EmulatorDownloadProgress?> = kotlinx.coroutines.flow.emptyFlow()
}
