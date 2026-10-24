package com.nendo.argosy.ui.screens.settings.delegates

import kotlinx.coroutines.Dispatchers
import com.nendo.argosy.util.SafeCoroutineScope
import com.nendo.argosy.platform.SandboxPermissions
import com.nendo.argosy.platform.PlatformManifest as Manifest
import com.nendo.argosy.platform.PlatformApplication as Application
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.className
import com.nendo.argosy.platform.PlatformIntent
import com.nendo.argosy.platform.PlatformPackageManagerConstants as PackageManager
import com.nendo.argosy.platform.PlatformUri as Uri
import com.nendo.argosy.platform.authority
import com.nendo.argosy.platform.encodedPath
import com.nendo.argosy.platform.getQueryParameter
import com.nendo.argosy.platform.host
import com.nendo.argosy.platform.lastPathSegment
import com.nendo.argosy.platform.path
import com.nendo.argosy.platform.pathSegments
import com.nendo.argosy.platform.queryParameterNames
import com.nendo.argosy.platform.scheme
import com.nendo.argosy.platform.PlatformUris
import com.nendo.argosy.platform.PlatformBuild as Build
import com.nendo.argosy.platform.PlatformSettings as Settings
import com.nendo.argosy.platform.PlatformContextCompat as ContextCompat
import com.nendo.argosy.platform.displayId
import com.nendo.argosy.data.wallpaper.LockScreenScenes
import com.nendo.argosy.hardware.FanController
import com.nendo.argosy.hardware.LEDController
import com.nendo.argosy.hardware.ScreenCaptureManager
import com.nendo.argosy.ui.screens.settings.PermissionsState
import com.nendo.argosy.data.preferences.DisplayPreferencesRepository
import com.nendo.argosy.util.PermissionHelper
import com.nendo.argosy.util.StorageAccess
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class PermissionsSettingsDelegate @Inject constructor(
    private val application: Application,
    private val permissionHelper: PermissionHelper,
    private val screenCaptureManager: ScreenCaptureManager,
    private val ledController: LEDController,
    private val fanController: FanController,
    private val displayPreferences: DisplayPreferencesRepository,
    private val sandboxPermissions: SandboxPermissions
) {
    private val _state = MutableStateFlow(PermissionsState(sandboxSupported = sandboxPermissions.isSupported))
    private val sandboxScope = SafeCoroutineScope(Dispatchers.IO, "PermissionsSettingsDelegate")
    val state: StateFlow<PermissionsState> = _state.asStateFlow()

    @Volatile private var lockScreenArt = true

    fun observeLockScreenArt(scope: CoroutineScope) {
        scope.launch {
            displayPreferences.preferences.map { it.lockScreenArt }.distinctUntilChanged().collect {
                lockScreenArt = it
                refreshPermissions()
            }
        }
    }

    fun refreshPermissions() {
        if (sandboxPermissions.isSupported) {
            sandboxScope.launch {
                val report = sandboxPermissions.read()
                _state.update { it.copy(sandbox = report) }
            }
        }
        val hasStorage = StorageAccess.isGranted(application)
        val hasUsageStats = permissionHelper.hasUsageStatsPermission(application)
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val hasWriteSettings = Settings.System.canWrite(application)
        val isDeviceWithFanControl = fanController.isAvailable()
        val hasScreenCapture = screenCaptureManager.hasPermission.value
        val isLedAvailable = ledController.isAvailable
        val hasDisplayOverlay = Settings.canDrawOverlays(application)

        _state.update {
            it.copy(
                hasStorageAccess = hasStorage,
                hasUsageStats = hasUsageStats,
                hasNotificationPermission = hasNotification,
                hasWriteSettings = hasWriteSettings,
                isWriteSettingsRelevant = isDeviceWithFanControl,
                hasScreenCapture = hasScreenCapture,
                isScreenCaptureRelevant = isLedAvailable,
                hasDisplayOverlay = hasDisplayOverlay,
                hasLiveWallpaper = LockScreenScenes.isLiveActive(application),
                isLiveWallpaperRelevant = LockScreenScenes.canOffer(application, lockScreenArt)
            )
        }
    }

    fun openLiveWallpaperPicker() {
        application.startActivity(LockScreenScenes.pickerIntent(application).addFlags(PlatformIntent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun observeScreenCapturePermission(scope: CoroutineScope) {
        scope.launch {
            screenCaptureManager.hasPermission.collect { hasPermission ->
                _state.update { it.copy(hasScreenCapture = hasPermission) }
            }
        }
    }

    fun hasScreenCapturePermission(): Boolean = screenCaptureManager.hasPermission.value

    fun isScreenCaptureRelevant(): Boolean = ledController.isAvailable

    fun openUsageStatsSettings() {
        permissionHelper.openUsageStatsSettings(application)
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, application.packageName)
            flags = PlatformIntent.FLAG_ACTIVITY_NEW_TASK
        }
        application.startActivity(intent)
    }

    fun openWriteSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = PlatformUris.parse("package:${application.packageName}")
            flags = PlatformIntent.FLAG_ACTIVITY_NEW_TASK
        }
        application.startActivity(intent)
    }

    fun openDisplayOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = PlatformUris.parse("package:${application.packageName}")
            flags = PlatformIntent.FLAG_ACTIVITY_NEW_TASK
        }
        application.startActivity(intent)
    }

    fun hasWriteSettingsPermission(): Boolean = Settings.System.canWrite(application)

    fun isDeviceSettingsSupported(): Boolean = fanController.isAvailable()
}
