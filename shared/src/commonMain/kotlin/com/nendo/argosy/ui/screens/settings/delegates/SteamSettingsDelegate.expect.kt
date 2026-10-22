package com.nendo.argosy.ui.screens.settings.delegates

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
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
import com.nendo.argosy.data.launcher.GameNativeSyncFolder
import com.nendo.argosy.data.steam.SteamService
import com.nendo.argosy.ui.screens.settings.SteamSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

expect class SteamSettingsDelegate {
    val state: StateFlow<SteamSettingsState>

    val requestStoragePermissionEvent: SharedFlow<Unit>

    val openUrlEvent: SharedFlow<String>

    val openStoreSyncDirPicker: SharedFlow<GameNativeSyncFolder>

    fun moveGameNativeActionFocus(delta: Int): Boolean

    fun openGameNativeFoldersModal()

    fun dismissGameNativeFoldersModal()

    fun moveGameNativeFoldersFocus(delta: Int)

    fun moveGameNativeFoldersActionFocus(delta: Int): Boolean

    fun confirmGameNativeFoldersRow(scope: CoroutineScope)

    fun openStoreSyncDirPicker(scope: CoroutineScope, folder: GameNativeSyncFolder)

    fun setStoreSyncDir(scope: CoroutineScope, folder: GameNativeSyncFolder, path: String)

    fun clearStoreSyncDir(scope: CoroutineScope, folder: GameNativeSyncFolder)

    fun rescanStoreSync(scope: CoroutineScope)

    val downloadProgress: kotlinx.coroutines.flow.Flow<com.nendo.argosy.data.emulator.EmulatorDownloadProgress?>

    fun updateState(newState: SteamSettingsState)

    fun bindService(service: SteamService, scope: CoroutineScope)

    fun loadSteamSettings(context: Context, scope: CoroutineScope)

    fun cycleSteamInstallVolume(scope: CoroutineScope, direction: Int = 1)

    fun installSteamLauncher(emulatorId: String, scope: CoroutineScope)

    fun connectToSteam(context: Context, scope: CoroutineScope)

    fun unbindService(context: Context)

    fun startQrAuth()

    fun cancelQrAuth()

    fun syncLibrary(context: Context, scope: CoroutineScope)

    fun forceSyncLibraryWithOverwrite(context: Context, scope: CoroutineScope)

    fun disconnectSteam(context: Context, scope: CoroutineScope)

    fun resetLibrary(scope: CoroutineScope)

    fun showAddSteamGameDialog()

    fun dismissAddSteamGameDialog()

    fun setAddGameAppId(appId: String)

    fun confirmAddSteamGame(context: Context, scope: CoroutineScope, launcherPackage: String = (com.nendo.argosy.data.launcher.GameNativeLauncher as com.nendo.argosy.data.launcher.SteamLauncher).packageName)
}
