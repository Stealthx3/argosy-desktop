package com.nendo.argosy.ui.screens.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.nendo.argosy.data.model.CatalogEmulator
import com.nendo.argosy.data.model.CatalogPackage
import com.nendo.argosy.data.platform.PlatformDefinitions
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.ui.components.ActionPreference
import com.nendo.argosy.ui.components.FocusedScroll
import com.nendo.argosy.ui.components.InfoPreference
import com.nendo.argosy.ui.components.Modal
import com.nendo.argosy.ui.primitives.ArgosyConfirmModalHost
import com.nendo.argosy.ui.primitives.FocusIndicators
import com.nendo.argosy.ui.primitives.argosyFocusIndicators
import com.nendo.argosy.ui.screens.settings.EmulatorManagerState
import com.nendo.argosy.ui.screens.settings.EmulatorPicker
import com.nendo.argosy.ui.screens.settings.EmulatorPickerKind
import com.nendo.argosy.ui.screens.settings.SettingsUiState
import com.nendo.argosy.ui.screens.settings.SettingsViewModel
import com.nendo.argosy.ui.screens.settings.components.SectionHeader
import com.nendo.argosy.ui.screens.settings.components.SectionPaneLayout
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.util.clickableNoFocus

internal sealed class EmulatorManagerItem(val key: String) {
    data class Platform(val slug: String, val name: String) : EmulatorManagerItem("platform-$slug")
    data class Entry(val emulator: CatalogEmulator, val platformSlug: String) :
        EmulatorManagerItem("emulator-$platformSlug-${emulator.id}")
    data object GitHubInstallers : EmulatorManagerItem("github-installers")

    val isFocusable: Boolean get() = this !is Platform
}

internal fun emulatorManagerItems(state: EmulatorManagerState): List<EmulatorManagerItem> = buildList {
    val bySlug = state.emulators
        .flatMap { emulator -> emulator.platforms.map { slug -> slug to emulator } }
        .groupBy({ it.first }, { it.second })
    bySlug.entries
        .sortedWith(compareBy({ PlatformDefinitions.getBySlug(it.key)?.sortOrder ?: Int.MAX_VALUE }, { it.key }))
        .forEach { (slug, emulators) ->
            add(EmulatorManagerItem.Platform(slug, PlatformDefinitions.getBySlug(slug)?.name ?: slug))
            emulators.sortedWith(compareBy({ it.installed == null }, { it.displayName.lowercase() }))
                .forEach { add(EmulatorManagerItem.Entry(it, slug)) }
        }
    add(EmulatorManagerItem.GitHubInstallers)
}

private fun focusableEmulatorItems(state: EmulatorManagerState): List<EmulatorManagerItem> =
    emulatorManagerItems(state).filter { it.isFocusable }

internal fun emulatorManagerMaxFocusIndex(state: EmulatorManagerState): Int =
    (focusableEmulatorItems(state).size - 1).coerceAtLeast(0)

internal fun emulatorManagerItemAtFocusIndex(index: Int, state: EmulatorManagerState): EmulatorManagerItem? =
    focusableEmulatorItems(state).getOrNull(index)

internal sealed class EmulatorDetailItem(val key: String, val isFocusable: Boolean) {
    data object Message : EmulatorDetailItem("message", false)
    data object Status : EmulatorDetailItem("status", false)
    data object Package : EmulatorDetailItem("package", false)
    data object EmuDeck : EmulatorDetailItem("emudeck", false)
    data object Platforms : EmulatorDetailItem("platforms", false)
    data object Logs : EmulatorDetailItem("logs", true)
    data object DataFolder : EmulatorDetailItem("data", true)
    data object Bios : EmulatorDetailItem("bios", true)
    data object Install : EmulatorDetailItem("install", true)
    data object Uninstall : EmulatorDetailItem("uninstall", true)
}

internal fun emulatorDetailItems(state: EmulatorManagerState, installerBusy: Boolean): List<EmulatorDetailItem> {
    val emulator = state.selected ?: return emptyList()
    return buildList {
        if (state.messageRes != null || installerBusy) add(EmulatorDetailItem.Message)
        add(EmulatorDetailItem.Status)
        add(EmulatorDetailItem.Package)
        add(EmulatorDetailItem.EmuDeck)
        add(EmulatorDetailItem.Platforms)
        add(EmulatorDetailItem.Logs)
        if (emulator.dataFolder != null) add(EmulatorDetailItem.DataFolder)
        add(EmulatorDetailItem.Bios)
        if (emulator.installed == null) add(EmulatorDetailItem.Install) else add(EmulatorDetailItem.Uninstall)
    }
}

private fun focusableDetailItems(state: EmulatorManagerState, installerBusy: Boolean): List<EmulatorDetailItem> =
    emulatorDetailItems(state, installerBusy).filter { it.isFocusable }

internal fun emulatorDetailMaxFocusIndex(state: EmulatorManagerState, installerBusy: Boolean): Int =
    (focusableDetailItems(state, installerBusy).size - 1).coerceAtLeast(0)

internal fun emulatorDetailItemAtFocusIndex(index: Int, state: EmulatorManagerState, installerBusy: Boolean): EmulatorDetailItem? =
    focusableDetailItems(state, installerBusy).getOrNull(index)

@Composable
private fun packageLabel(kind: CatalogPackage?): String = when (kind) {
    CatalogPackage.FLATPAK -> stringResource(R.string.settings_emulator_manager_package_flatpak)
    CatalogPackage.APPIMAGE -> stringResource(R.string.settings_emulator_manager_package_appimage)
    CatalogPackage.NATIVE -> stringResource(R.string.settings_emulator_manager_package_native)
    null -> stringResource(R.string.settings_emulator_manager_package_none)
}

@Composable
private fun statusLabel(emulator: CatalogEmulator): String {
    val base = when {
        emulator.installed == null -> stringResource(R.string.settings_emulator_manager_status_not_installed)
        emulator.version.isNullOrBlank() -> stringResource(
            R.string.settings_emulator_manager_status_installed_no_version,
            packageLabel(emulator.installed)
        )
        else -> stringResource(
            R.string.settings_emulator_manager_status_installed,
            packageLabel(emulator.installed),
            emulator.version
        )
    }
    return if (emulator.emuDeckManaged) stringResource(R.string.settings_emulator_manager_status_emudeck, base) else base
}

@Composable
fun EmulatorManagerSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val state = uiState.emulatorManager
    val items = remember(state.emulators) { emulatorManagerItems(state) }
    val focusable = remember(items) { items.filter { it.isFocusable } }
    val focusedItem = focusable.getOrNull(uiState.focusedIndex)

    if (state.loading) {
        Text(
            text = stringResource(R.string.settings_emulator_manager_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalArgosyTheme.current.textDim,
            modifier = Modifier.padding(Dimens.spacingLg)
        )
        return
    }

    SectionPaneLayout(
        items = items,
        sections = emptyList(),
        focusedIndex = uiState.focusedIndex,
        focusToListIndex = { index -> focusable.getOrNull(index)?.let { items.indexOf(it) } ?: 0 },
        itemKey = { it.key },
        isNavItem = { false },
        isHeader = { it is EmulatorManagerItem.Platform },
        onSectionTap = {},
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) { item ->
        when (item) {
            is EmulatorManagerItem.Platform -> SectionHeader(item.name)
            is EmulatorManagerItem.Entry -> ActionPreference(
                title = item.emulator.displayName,
                subtitle = statusLabel(item.emulator),
                icon = if (item.emulator.installed != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                isFocused = item == focusedItem,
                onClick = { viewModel.openEmulatorDetail(item.emulator.id) }
            )
            EmulatorManagerItem.GitHubInstallers -> ActionPreference(
                title = stringResource(R.string.settings_emulator_manager_github_title),
                subtitle = stringResource(R.string.settings_emulator_manager_github_subtitle),
                icon = Icons.Default.Storefront,
                isFocused = item == focusedItem,
                onClick = { viewModel.openGitHubInstallers() }
            )
        }
    }
}

@Composable
fun EmulatorManagerDetailSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val state = uiState.emulatorManager
    val emulator = state.selected ?: return
    val installers = uiState.managedInstallers
    val installerBusy = installers.busyId != null
    val items = emulatorDetailItems(state, installerBusy)
    val focusable = items.filter { it.isFocusable }
    val focusedItem = focusable.getOrNull(uiState.focusedIndex)

    LaunchedEffect(installerBusy) {
        if (!installerBusy) viewModel.refreshEmulatorManager()
    }

    LaunchedEffect(focusable.size) {
        if (focusable.isNotEmpty() && uiState.focusedIndex > focusable.lastIndex) viewModel.setFocusIndex(focusable.lastIndex)
    }

    SectionPaneLayout(
        items = items,
        sections = emptyList(),
        focusedIndex = uiState.focusedIndex,
        focusToListIndex = { index -> if (index == 0) 0 else focusable.getOrNull(index)?.let { items.indexOf(it) } ?: 0 },
        itemKey = { it.key },
        isNavItem = { false },
        onSectionTap = {},
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) { item ->
        when (item) {
            EmulatorDetailItem.Message -> {
                val message = when {
                    installerBusy && installers.statusRes == R.string.settings_installers_status_downloading ->
                        stringResource(installers.statusRes, (installers.busyProgress * 100).toInt())
                    installerBusy && installers.statusRes != null -> stringResource(installers.statusRes)
                    state.messageRes != null -> stringResource(state.messageRes)
                    else -> ""
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalArgosyTheme.current.textDim,
                    modifier = Modifier.padding(horizontal = Dimens.spacingSm)
                )
            }
            EmulatorDetailItem.Status -> InfoPreference(
                title = stringResource(R.string.settings_emulator_manager_row_status),
                value = statusLabel(emulator),
                isFocused = false
            )
            EmulatorDetailItem.Package -> InfoPreference(
                title = stringResource(R.string.settings_emulator_manager_row_package),
                value = packageLabel(emulator.installed),
                isFocused = false
            )
            EmulatorDetailItem.EmuDeck -> InfoPreference(
                title = stringResource(R.string.settings_emulator_manager_row_emudeck),
                value = stringResource(
                    if (emulator.emuDeckManaged) R.string.settings_emulator_manager_emudeck_yes else R.string.settings_emulator_manager_emudeck_no
                ),
                isFocused = false
            )
            EmulatorDetailItem.Platforms -> InfoPreference(
                title = stringResource(R.string.settings_emulator_manager_row_platforms),
                value = emulator.platforms.size.toString(),
                subtitle = emulator.platforms.joinToString { PlatformDefinitions.getBySlug(it)?.shortName ?: it },
                isFocused = false
            )
            EmulatorDetailItem.Logs -> ActionPreference(
                title = stringResource(R.string.settings_emulator_manager_row_logs),
                subtitle = emulator.logFolder,
                icon = Icons.Default.Description,
                isFocused = item == focusedItem,
                onClick = { viewModel.openEmulatorLogs() }
            )
            EmulatorDetailItem.DataFolder -> ActionPreference(
                title = stringResource(R.string.settings_emulator_manager_row_data),
                subtitle = emulator.dataFolder.orEmpty(),
                icon = Icons.Default.Folder,
                isFocused = item == focusedItem,
                onClick = { viewModel.openEmulatorDataFolder() }
            )
            EmulatorDetailItem.Bios -> ActionPreference(
                title = stringResource(R.string.settings_emulator_manager_row_bios),
                subtitle = emulator.biosFolder?.let { stringResource(R.string.settings_emulator_manager_row_bios_subtitle, it) }
                    ?: stringResource(R.string.settings_emulator_manager_row_bios_unavailable),
                icon = Icons.Default.Memory,
                isFocused = item == focusedItem,
                isEnabled = emulator.biosFolder != null && !state.busy,
                onClick = { viewModel.requestEmulatorBios() }
            )
            EmulatorDetailItem.Install -> ActionPreference(
                title = stringResource(R.string.settings_emulator_manager_row_install),
                subtitle = if (emulator.installOptions.isEmpty()) {
                    stringResource(R.string.settings_emulator_manager_row_install_none)
                } else {
                    emulator.installOptions.map { packageLabel(it) }.joinToString(" / ")
                },
                icon = if (state.busy || installerBusy) Icons.Default.Autorenew else Icons.Default.Download,
                spinIcon = state.busy || installerBusy,
                isFocused = item == focusedItem,
                isEnabled = emulator.installOptions.isNotEmpty() && !state.busy && !installerBusy,
                onClick = { viewModel.requestEmulatorInstall() }
            )
            EmulatorDetailItem.Uninstall -> ActionPreference(
                title = stringResource(R.string.settings_emulator_manager_row_uninstall),
                subtitle = if (emulator.installed == CatalogPackage.NATIVE) {
                    stringResource(R.string.settings_emulator_manager_row_uninstall_native)
                } else {
                    packageLabel(emulator.installed)
                },
                icon = Icons.Default.Delete,
                isDangerous = true,
                spinIcon = state.busy,
                isFocused = item == focusedItem,
                isEnabled = emulator.installed != CatalogPackage.NATIVE && !state.busy,
                onClick = { viewModel.requestEmulatorUninstall() }
            )
        }
    }

    state.picker?.let { picker ->
        EmulatorPickerModal(
            picker = picker,
            onItemTap = { viewModel.selectEmulatorPickerOption(it) },
            onDismiss = { viewModel.dismissEmulatorManagerPicker() }
        )
    }

    ArgosyConfirmModalHost(
        visible = state.confirmUninstall,
        title = stringResource(R.string.settings_emulator_manager_uninstall_title, emulator.displayName),
        message = stringResource(
            if (emulator.emuDeckManaged) {
                R.string.settings_emulator_manager_uninstall_message_emudeck
            } else {
                R.string.settings_emulator_manager_uninstall_message
            }
        ),
        confirmLabel = stringResource(R.string.settings_emulator_manager_uninstall_confirm),
        destructive = true,
        onConfirm = { viewModel.confirmEmulatorUninstall() },
        onDismiss = { viewModel.dismissEmulatorUninstall() }
    )

    ManagedInstallerModals(installers, viewModel)
}

@Composable
private fun EmulatorPickerModal(picker: EmulatorPicker, onItemTap: (Int) -> Unit, onDismiss: () -> Unit) {
    val listState = rememberLazyListState()
    FocusedScroll(listState = listState, focusedIndex = picker.focusIndex)
    Modal(
        title = stringResource(
            when (picker.kind) {
                EmulatorPickerKind.INSTALL_SOURCE -> R.string.settings_emulator_manager_picker_install_title
                EmulatorPickerKind.BIOS_SOURCE -> R.string.settings_emulator_manager_picker_bios_title
                EmulatorPickerKind.FIRMWARE -> R.string.settings_emulator_manager_picker_firmware_title
            }
        ),
        baseWidth = Dimens.modalWidthXl,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(Dimens.listGap)
        ) {
            itemsIndexed(picker.options, key = { index, option -> "$index-${option.token}" }) { index, option ->
                val shape = RoundedCornerShape(Dimens.radiusControl)
                Text(
                    text = option.labelRes?.let { stringResource(it) } ?: option.label.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalArgosyTheme.current.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .argosyFocusIndicators(
                            focused = index == picker.focusIndex,
                            indicators = FocusIndicators.ListRow,
                            shape = shape
                        )
                        .clickableNoFocus(onClick = { onItemTap(index) })
                        .padding(Dimens.spacingSm)
                )
            }
        }
    }
}
