package com.nendo.argosy.ui.screens.settings.delegates

import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.NotificationType
import com.nendo.argosy.data.emulator.EmulatorCatalog
import com.nendo.argosy.data.installer.ManagedInstallerRepository
import com.nendo.argosy.data.model.CatalogPackage
import com.nendo.argosy.data.repository.BiosRepository
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.ui.screens.settings.EmulatorManagerState
import com.nendo.argosy.ui.screens.settings.EmulatorPicker
import com.nendo.argosy.ui.screens.settings.EmulatorPickerKind
import com.nendo.argosy.ui.screens.settings.EmulatorPickerOption
import com.nendo.argosy.ui.screens.settings.PICKER_TOKEN_APPIMAGE
import com.nendo.argosy.ui.screens.settings.PICKER_TOKEN_FILE
import com.nendo.argosy.ui.screens.settings.PICKER_TOKEN_FLATPAK
import com.nendo.argosy.ui.screens.settings.PICKER_TOKEN_ROMM
import com.nendo.argosy.ui.screens.settings.pickerToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class EmulatorManagerDelegate @Inject constructor(
    private val catalog: EmulatorCatalog,
    private val biosRepository: BiosRepository,
    private val installerRepository: ManagedInstallerRepository,
    private val notificationManager: NotificationManager
) {
    private var announcing = false

    private val _state = MutableStateFlow(EmulatorManagerState(isSupported = catalog.isSupported))
    val state: StateFlow<EmulatorManagerState> = _state.asStateFlow()

    private val _pickBiosFile = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val pickBiosFile: SharedFlow<String> = _pickBiosFile.asSharedFlow()

    private val _installAppImage = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val installAppImage: SharedFlow<Long> = _installAppImage.asSharedFlow()

    fun load(scope: CoroutineScope) {
        if (!catalog.isSupported) return
        if (!announcing) {
            announcing = true
            scope.launch {
                _state.map { it.messageRes }.distinctUntilChanged().filterNotNull().collect { announce(it) }
            }
        }
        _state.update { it.copy(loading = it.emulators.isEmpty()) }
        scope.launch {
            val entries = catalog.entries()
            _state.update { current ->
                val pending = current.messageRes == R.string.settings_emulator_manager_message_installing && !current.busy
                val installed = entries.firstOrNull { it.id == current.selectedId }?.installed != null
                current.copy(
                    loading = false,
                    emulators = entries,
                    messageRes = if (pending && installed) R.string.settings_emulator_manager_message_installed else current.messageRes
                )
            }
        }
    }

    private fun announce(messageRes: Int) {
        val type = when (messageRes) {
            R.string.settings_emulator_manager_message_installing,
            R.string.settings_emulator_manager_message_uninstalling -> NotificationType.INFO
            R.string.settings_emulator_manager_message_installed,
            R.string.settings_emulator_manager_message_uninstalled,
            R.string.settings_emulator_manager_message_bios_copied -> NotificationType.SUCCESS
            else -> NotificationType.WARNING
        }
        val title = _state.value.selected?.displayName
        notificationManager.show(
            title = if (title != null) NotificationText.Raw(title) else NotificationText.Res(messageRes),
            subtitle = if (title != null) NotificationText.Res(messageRes) else null,
            type = type
        )
    }

    fun appImageInstallFailed() {
        if (_state.value.messageRes == R.string.settings_emulator_manager_message_installing && !_state.value.busy) {
            _state.update { it.copy(messageRes = R.string.settings_emulator_manager_message_install_failed) }
        }
    }

    fun select(id: String) {
        _state.update { it.copy(selectedId = id, messageRes = null, picker = null, confirmUninstall = false) }
    }

    fun requestInstall(scope: CoroutineScope) {
        val selected = _state.value.selected ?: return
        when (selected.installOptions.size) {
            0 -> _state.update { it.copy(messageRes = R.string.settings_emulator_manager_message_no_install) }
            1 -> install(scope, selected.installOptions.single().pickerToken())
            else -> _state.update {
                it.copy(
                    picker = EmulatorPicker(
                        kind = EmulatorPickerKind.INSTALL_SOURCE,
                        options = selected.installOptions.map { option ->
                            EmulatorPickerOption(
                                token = option.pickerToken(),
                                labelRes = if (option == CatalogPackage.FLATPAK) {
                                    R.string.settings_emulator_manager_install_flatpak
                                } else {
                                    R.string.settings_emulator_manager_install_appimage
                                }
                            )
                        }
                    )
                )
            }
        }
    }

    private fun install(scope: CoroutineScope, token: String) {
        val selected = _state.value.selected ?: return
        when (token) {
            PICKER_TOKEN_FLATPAK -> {
                _state.update { it.copy(busy = true, messageRes = R.string.settings_emulator_manager_message_installing) }
                scope.launch {
                    val ok = catalog.installFlatpak(selected.id)
                    val entries = catalog.entries()
                    _state.update {
                        it.copy(
                            busy = false,
                            emulators = entries,
                            messageRes = if (ok) R.string.settings_emulator_manager_message_installed else R.string.settings_emulator_manager_message_install_failed
                        )
                    }
                }
            }
            PICKER_TOKEN_APPIMAGE -> scope.launch {
                installerRepository.ensureSeeded()
                val repo = selected.appImageRepo
                val row = repo?.let { r ->
                    installerRepository.getAll().firstOrNull { "${it.repoOwner}/${it.repoName}".equals(r, ignoreCase = true) }
                }
                if (row == null) {
                    _state.update { it.copy(messageRes = R.string.settings_emulator_manager_message_install_failed) }
                } else {
                    _state.update { it.copy(messageRes = R.string.settings_emulator_manager_message_installing) }
                    _installAppImage.emit(row.id)
                }
            }
        }
    }

    fun requestUninstall() {
        if (_state.value.selected?.installed == null) return
        _state.update { it.copy(confirmUninstall = true) }
    }

    fun dismissUninstall() {
        _state.update { it.copy(confirmUninstall = false) }
    }

    fun confirmUninstall(scope: CoroutineScope) {
        val selected = _state.value.selected ?: return
        _state.update { it.copy(confirmUninstall = false, busy = true, messageRes = R.string.settings_emulator_manager_message_uninstalling) }
        scope.launch {
            val ok = catalog.uninstall(selected.id)
            val entries = catalog.entries()
            _state.update {
                it.copy(
                    busy = false,
                    emulators = entries,
                    messageRes = if (ok) R.string.settings_emulator_manager_message_uninstalled else R.string.settings_emulator_manager_message_uninstall_failed
                )
            }
        }
    }

    fun openLogs() {
        _state.value.selected?.let { catalog.openFolder(it.logFolder) }
    }

    fun openDataFolder() {
        _state.value.selected?.dataFolder?.let { catalog.openFolder(it) }
    }

    fun requestBios() {
        val selected = _state.value.selected ?: return
        if (selected.biosFolder == null) {
            _state.update { it.copy(messageRes = R.string.settings_emulator_manager_message_bios_unavailable) }
            return
        }
        _state.update {
            it.copy(
                picker = EmulatorPicker(
                    kind = EmulatorPickerKind.BIOS_SOURCE,
                    options = listOf(
                        EmulatorPickerOption(PICKER_TOKEN_ROMM, labelRes = R.string.settings_emulator_manager_bios_from_romm),
                        EmulatorPickerOption(PICKER_TOKEN_FILE, labelRes = R.string.settings_emulator_manager_bios_from_file)
                    )
                )
            )
        }
    }

    fun movePicker(delta: Int) {
        _state.update { current ->
            val picker = current.picker ?: return@update current
            current.copy(picker = picker.copy(focusIndex = (picker.focusIndex + delta).mod(picker.options.size)))
        }
    }

    fun dismissPicker() {
        _state.update { it.copy(picker = null) }
    }

    fun confirmPicker(scope: CoroutineScope) {
        val picker = _state.value.picker ?: return
        pickerSelect(scope, picker.focusIndex)
    }

    fun pickerSelect(scope: CoroutineScope, index: Int) {
        val picker = _state.value.picker ?: return
        val option = picker.options.getOrNull(index) ?: return
        _state.update { it.copy(picker = null) }
        when (picker.kind) {
            EmulatorPickerKind.INSTALL_SOURCE -> install(scope, option.token)
            EmulatorPickerKind.BIOS_SOURCE -> if (option.token == PICKER_TOKEN_ROMM) openFirmwareList(scope) else {
                _state.value.selected?.let { _pickBiosFile.tryEmit(it.displayName) }
            }
            EmulatorPickerKind.FIRMWARE -> copyBios(scope, option.token)
        }
    }

    private fun openFirmwareList(scope: CoroutineScope) {
        val selected = _state.value.selected ?: return
        scope.launch {
            val platforms = selected.platforms.toSet()
            val firmware = biosRepository.observeFirmware().first()
                .filter { it.platformSlug in platforms && it.localPath != null }
            if (firmware.isEmpty()) {
                _state.update { it.copy(messageRes = R.string.settings_emulator_manager_message_no_firmware) }
            } else {
                _state.update {
                    it.copy(
                        picker = EmulatorPicker(
                            kind = EmulatorPickerKind.FIRMWARE,
                            options = firmware.map { entry -> EmulatorPickerOption(token = entry.localPath.orEmpty(), label = entry.fileName) }
                        )
                    )
                }
            }
        }
    }

    fun copyBios(scope: CoroutineScope, sourcePath: String) {
        val folder = _state.value.selected?.biosFolder ?: return
        _state.update { it.copy(busy = true) }
        scope.launch {
            val ok = catalog.copyBios(sourcePath, folder)
            _state.update {
                it.copy(
                    busy = false,
                    messageRes = if (ok) R.string.settings_emulator_manager_message_bios_copied else R.string.settings_emulator_manager_message_bios_failed
                )
            }
        }
    }
}
