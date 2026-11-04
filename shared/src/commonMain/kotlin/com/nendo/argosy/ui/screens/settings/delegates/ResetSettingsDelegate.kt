package com.nendo.argosy.ui.screens.settings.delegates

import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.showError
import com.nendo.argosy.core.notification.showSuccess
import com.nendo.argosy.data.installer.ManagedInstallerManager
import com.nendo.argosy.data.installer.ManagedInstallerRepository
import com.nendo.argosy.data.platform.PlatformFolderResolver
import com.nendo.argosy.data.repository.DatabaseAdminRepository
import com.nendo.argosy.data.repository.HardResetBlocker
import com.nendo.argosy.platform.AppShutdown
import com.nendo.argosy.res.ArgosyR as R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ResetDialog { PLATFORMS, FACTORY_WARNING, FACTORY_CONFIRM }

class ResetSettingsDelegate @Inject constructor(
    private val databaseAdminRepository: DatabaseAdminRepository,
    private val folderResolver: PlatformFolderResolver,
    private val installerRepository: ManagedInstallerRepository,
    private val installerManager: ManagedInstallerManager,
    private val notificationManager: NotificationManager
) {
    private val _dialog = MutableStateFlow<ResetDialog?>(null)
    val dialog: StateFlow<ResetDialog?> = _dialog.asStateFlow()

    private val _pendingSaveUploads = MutableStateFlow(0)
    val pendingSaveUploads: StateFlow<Int> = _pendingSaveUploads.asStateFlow()

    private var factoryResetRunning = false

    fun requestPlatformReset() {
        _dialog.value = ResetDialog.PLATFORMS
    }

    fun dismiss() {
        _dialog.value = null
    }

    fun confirmPlatformReset(scope: CoroutineScope, onDone: () -> Unit) {
        _dialog.value = null
        scope.launch {
            databaseAdminRepository.resetPlatformConfigurations()
            onDone()
            notificationManager.showSuccess(NotificationText.Res(R.string.settings_emulators_reset_done))
        }
    }

    fun requestFactoryReset(scope: CoroutineScope) {
        scope.launch {
            _pendingSaveUploads.value = databaseAdminRepository.pendingSaveUploadCount()
            _dialog.value = ResetDialog.FACTORY_WARNING
        }
    }

    fun continueFactoryReset() {
        _dialog.value = ResetDialog.FACTORY_CONFIRM
    }

    fun confirmFactoryReset(scope: CoroutineScope, blockerMessage: (HardResetBlocker) -> NotificationText) {
        _dialog.value = null
        if (factoryResetRunning) return
        factoryResetRunning = true
        scope.launch {
            val blocker = databaseAdminRepository.factoryResetBlocker()
            if (blocker != null) {
                factoryResetRunning = false
                notificationManager.showError(blockerMessage(blocker))
                return@launch
            }
            databaseAdminRepository.deleteFactoryResetFiles(
                storageBase = folderResolver.storageBase(),
                defaultStorageBase = folderResolver.defaultStorageBase
            )
            installerManager.removeInstalledApps(installerRepository.getAll())
            AppShutdown.requestFactoryReset()
        }
    }
}
