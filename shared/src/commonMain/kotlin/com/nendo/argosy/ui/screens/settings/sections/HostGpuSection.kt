package com.nendo.argosy.ui.screens.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nendo.argosy.data.model.HostGpuInfo
import com.nendo.argosy.data.model.HostGpuType
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.ui.components.FocusedScroll
import com.nendo.argosy.ui.components.InfoPreference
import com.nendo.argosy.ui.screens.settings.components.LocalSettingsTouchFocus
import com.nendo.argosy.ui.screens.settings.SettingsUiState
import com.nendo.argosy.ui.theme.Dimens

internal sealed class HostGpuRow(val focusable: Boolean) {
    class Header(val title: String?, val titleRes: Int?) : HostGpuRow(focusable = false)
    class Info(val labelRes: Int, val value: HostGpuValue) : HostGpuRow(focusable = true)
}

internal sealed class HostGpuValue {
    class Text(val text: String) : HostGpuValue()
    class Res(val res: Int, val args: List<Any> = emptyList()) : HostGpuValue()
}

internal fun hostGpuRows(info: HostGpuInfo?): List<HostGpuRow> {
    if (info == null) return emptyList()
    val unknown = HostGpuValue.Res(R.string.settings_host_gpu_unknown)
    fun text(value: String?) = value?.let { HostGpuValue.Text(it) } ?: unknown
    val rows = mutableListOf<HostGpuRow>()
    if (info.gpus.isEmpty()) {
        rows += HostGpuRow.Info(R.string.settings_host_gpu_gpu, unknown)
    }
    info.gpus.forEach { gpu ->
        rows += HostGpuRow.Header(gpu.name, null)
        rows += HostGpuRow.Info(R.string.settings_host_gpu_vendor, text(gpu.vendor))
        rows += HostGpuRow.Info(R.string.settings_host_gpu_type, HostGpuValue.Res(gpu.type.labelRes))
        rows += HostGpuRow.Info(R.string.settings_host_gpu_kernel_driver, text(gpu.kernelDriver))
        rows += HostGpuRow.Info(R.string.settings_host_gpu_driver, text(gpu.driver))
        rows += HostGpuRow.Info(
            R.string.settings_host_gpu_vram,
            gpu.vramMb?.let { HostGpuValue.Res(R.string.settings_host_gpu_vram_value, listOf(it)) } ?: unknown
        )
        rows += HostGpuRow.Info(R.string.settings_host_gpu_vulkan, text(gpu.vulkanVersion))
        rows += HostGpuRow.Info(R.string.settings_host_gpu_opengl, text(gpu.openGlVersion))
    }
    rows += HostGpuRow.Header(null, R.string.settings_host_gpu_displays)
    if (info.displays.isEmpty()) {
        rows += HostGpuRow.Info(R.string.settings_host_gpu_displays, unknown)
    }
    info.displays.forEach { display ->
        val resolution = if (display.width != null && display.height != null) {
            "${display.width} × ${display.height}"
        } else {
            null
        }
        val value = when {
            resolution != null && display.diagonalInches != null -> HostGpuValue.Res(
                R.string.settings_host_gpu_display_value,
                listOf(resolution, display.diagonalInches.toInt())
            )
            else -> text(resolution)
        }
        rows += HostGpuRow.Info(R.string.settings_host_gpu_display, value)
    }
    return rows
}

private val HostGpuType.labelRes: Int
    get() = when (this) {
        HostGpuType.DISCRETE -> R.string.settings_host_gpu_type_discrete
        HostGpuType.INTEGRATED -> R.string.settings_host_gpu_type_integrated
        HostGpuType.VIRTUAL -> R.string.settings_host_gpu_type_virtual
        HostGpuType.OTHER -> R.string.settings_host_gpu_type_other
    }

@Composable
private fun HostGpuValue.resolve(): String = when (this) {
    is HostGpuValue.Text -> text
    is HostGpuValue.Res -> stringResource(res, *args.toTypedArray())
}

@Composable
internal fun HostGpuSection(uiState: SettingsUiState) {
    val listState = rememberLazyListState()
    val rows = hostGpuRows(uiState.drivers.hostGpu)
    val focusableRows = rows.withIndex().filter { it.value.focusable }.map { it.index }
    val focusedRow = focusableRows.getOrNull(uiState.focusedIndex)

    FocusedScroll(
        listState = listState,
        focusedIndex = focusedRow ?: 0,
        isTouchMode = LocalSettingsTouchFocus.current.isTouchMode
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        if (uiState.drivers.hostGpu == null) {
            item(key = "loading") {
                Text(
                    text = stringResource(R.string.settings_drivers_summary_checking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        itemsIndexed(rows) { index, row ->
            when (row) {
                is HostGpuRow.Header -> Text(
                    text = (row.title ?: row.titleRes?.let { stringResource(it) }).orEmpty().uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Dimens.spacingSm)
                )
                is HostGpuRow.Info -> InfoPreference(
                    title = stringResource(row.labelRes),
                    value = row.value.resolve(),
                    isFocused = index == focusedRow
                )
            }
        }
    }
}
