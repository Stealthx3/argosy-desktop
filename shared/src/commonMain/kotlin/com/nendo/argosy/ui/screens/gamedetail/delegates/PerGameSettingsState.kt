package com.nendo.argosy.ui.screens.gamedetail.delegates

import com.nendo.argosy.data.emulator.GpuDriverToken
import com.nendo.argosy.ui.common.GpuDriverBlock
import com.nendo.argosy.ui.common.GpuDriverPickerEntry
import com.nendo.argosy.ui.common.GraphicsBackendOptionUi
import com.nendo.argosy.ui.common.VulkanDriverOption
import com.nendo.argosy.ui.common.gpuDriverBlock
import com.nendo.argosy.ui.common.gpuDriverPickerEntries
import com.nendo.argosy.data.emulator.ExtensionOption
import com.nendo.argosy.data.preferences.EmulatorDisplayTarget
import com.nendo.argosy.data.sync.platform.MemcardInfo

data class PerGameSettingsState(
    val visible: Boolean = false,
    val focusIndex: Int = 0,
    val pathButtonIndex: Int = 0,
    val showPathBrowser: Boolean = false,
    val emulatorName: String? = null,
    val isEmulatorOverride: Boolean = false,
    val showCoreRow: Boolean = false,
    val coreName: String? = null,
    val isCoreOverride: Boolean = false,
    val showSavePathRow: Boolean = false,
    val showSaveBasePathRow: Boolean = false,
    val saveBasePath: String? = null,
    val saveBasePathIsInherited: Boolean = false,
    val savePath: String? = null,
    val isSavePathOverride: Boolean = false,
    val showDisplayTargetRow: Boolean = false,
    val displayTarget: EmulatorDisplayTarget? = null,
    val inheritedDisplayTarget: EmulatorDisplayTarget = EmulatorDisplayTarget.PRESENTATION,
    val extensionOptions: List<ExtensionOption> = emptyList(),
    val preferredExtension: String? = null,
    val inheritedExtension: String? = null,
    val showMemcardRow: Boolean = false,
    val memcardCards: List<MemcardInfo> = emptyList(),
    val selectedMemcardPath: String? = null,
    val inheritedMemcardName: String? = null,
    val showMemcardPicker: Boolean = false,
    val memcardPickerFocusIndex: Int = 0,
    val showGpuDriverRow: Boolean = false,
    val gpuDriver: String? = null,
    val inheritedGpuDriver: String = GpuDriverToken.SYSTEM,
    val gpuDrivers: List<VulkanDriverOption> = emptyList(),
    val gpuDriverBlock: GpuDriverBlock? = null,
    val graphicsCoreName: String? = null,
    val gpuDriverInstallable: List<String> = emptyList(),
    val installingGpuDriver: String? = null,
    val showGpuDriverPicker: Boolean = false,
    val backendOptions: List<GraphicsBackendOptionUi> = emptyList(),
    val gameBackend: String? = null,
    val inheritedBackend: String? = null
) {
    val rows: List<PerGameSettingsRow>
        get() = buildList {
            add(PerGameSettingsRow.EMULATOR)
            if (showCoreRow) add(PerGameSettingsRow.CORE)
            if (showSavePathRow) add(PerGameSettingsRow.SAVE_PATH)
            if (showSaveBasePathRow) add(PerGameSettingsRow.SAVE_BASE_PATH)
            if (showMemcardRow) add(PerGameSettingsRow.MEMCARD)
            if (showDisplayTargetRow) add(PerGameSettingsRow.DISPLAY_TARGET)
            if (extensionOptions.isNotEmpty()) add(PerGameSettingsRow.EXTENSION)
            if (backendOptions.isNotEmpty()) add(PerGameSettingsRow.BACKEND)
            if (showGpuDriverRow) add(PerGameSettingsRow.GPU_DRIVER)
            add(PerGameSettingsRow.PLATFORM_SETTINGS)
        }

    val gpuDriverCycle: List<String?>
        get() = listOf(null, GpuDriverToken.SYSTEM) + gpuDrivers.map { GpuDriverToken.custom(it.id) }

    val gpuDriverPicker: List<GpuDriverPickerEntry>
        get() = gpuDriverPickerEntries(gpuDriverCycle, gpuDriverInstallable)

    val backendCycle: List<String?>
        get() = listOf<String?>(null) + backendOptions.map { it.token }

    val focusedRow: PerGameSettingsRow? get() = rows.getOrNull(focusIndex)

    val memcardPickerCards: List<MemcardInfo>
        get() = listOf(
            MemcardInfo(
                name = inheritedMemcardName ?: "",
                path = "",
                gameFolderCount = 0,
                lastModified = 0L,
                isDefault = true
            )
        ) + memcardCards
}
