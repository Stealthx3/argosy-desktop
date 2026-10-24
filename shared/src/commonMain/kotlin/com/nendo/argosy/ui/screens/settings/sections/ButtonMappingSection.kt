package com.nendo.argosy.ui.screens.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.nendo.argosy.data.preferences.NavigationButtonMaps
import com.nendo.argosy.core.input.AndroidKeyCodes as KeyEvent
import com.nendo.argosy.libretro.HotkeyManager
import com.nendo.argosy.platform.LocalPlatformContext as LocalContext
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.ui.components.ActionPreference
import com.nendo.argosy.ui.components.InfoPreference
import com.nendo.argosy.ui.components.Modal
import com.nendo.argosy.ui.components.SwitchPreference
import com.nendo.argosy.ui.input.NavigationAction
import com.nendo.argosy.ui.screens.settings.ButtonMappingCapture
import com.nendo.argosy.ui.screens.settings.ButtonMappingTarget
import com.nendo.argosy.ui.screens.settings.ControlsState
import com.nendo.argosy.ui.screens.settings.SettingsUiState
import com.nendo.argosy.ui.screens.settings.SettingsViewModel
import com.nendo.argosy.ui.screens.settings.components.SectionPaneLayout
import com.nendo.argosy.ui.screens.settings.menu.SettingsLayout
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import kotlinx.coroutines.delay

internal sealed class ButtonMappingItem(
    val key: String,
    val section: String,
    val visibleWhen: (ControlsState) -> Boolean = { true }
) {
    val isFocusable: Boolean get() = this !is Header && this !is SectionSpacer

    class Header(key: String, section: String, val titleRes: Int) : ButtonMappingItem(key, section)

    class SectionSpacer(key: String, section: String) : ButtonMappingItem(key, section)

    data object ControllerInfo : ButtonMappingItem("controllerInfo", SECTION_CONTROLLER)

    data class ActionRow(val target: ButtonMappingTarget, val action: NavigationAction) : ButtonMappingItem(
        key = "${target.name}_${action.name}",
        section = sectionOf(target),
        visibleWhen = { target == ButtonMappingTarget.KEYBOARD || it.buttonMappingController != null }
    )

    data class Reset(val target: ButtonMappingTarget) : ButtonMappingItem(
        key = "reset_${target.name}",
        section = sectionOf(target),
        visibleWhen = { target == ButtonMappingTarget.KEYBOARD || it.buttonMappingController != null }
    )

    data object SwapAB : ButtonMappingItem("swapAB", SECTION_PRESETS)
    data object SwapXY : ButtonMappingItem("swapXY", SECTION_PRESETS)
    data object SwapStartSelect : ButtonMappingItem("swapStartSelect", SECTION_PRESETS)

    companion object {
        const val SECTION_CONTROLLER = "controller"
        const val SECTION_PRESETS = "presets"
        const val SECTION_KEYBOARD = "keyboard"

        fun sectionOf(target: ButtonMappingTarget): String = when (target) {
            ButtonMappingTarget.CONTROLLER -> SECTION_CONTROLLER
            ButtonMappingTarget.KEYBOARD -> SECTION_KEYBOARD
        }

        private fun rows(target: ButtonMappingTarget): List<ButtonMappingItem> =
            NavigationAction.entries.map { ActionRow(target, it) } + Reset(target)

        val ALL: List<ButtonMappingItem> = buildList {
            add(Header("controllerHeader", SECTION_CONTROLLER, R.string.settings_button_mapping_section_controller))
            add(ControllerInfo)
            addAll(rows(ButtonMappingTarget.CONTROLLER))
            add(SectionSpacer("presetsSpacer", SECTION_PRESETS))
            add(Header("presetsHeader", SECTION_PRESETS, R.string.settings_button_mapping_section_presets))
            add(SwapAB)
            add(SwapXY)
            add(SwapStartSelect)
            add(SectionSpacer("keyboardSpacer", SECTION_KEYBOARD))
            add(Header("keyboardHeader", SECTION_KEYBOARD, R.string.settings_button_mapping_section_keyboard))
            addAll(rows(ButtonMappingTarget.KEYBOARD))
        }
    }
}

private val buttonMappingLayout = SettingsLayout<ButtonMappingItem, ControlsState>(
    allItems = ButtonMappingItem.ALL,
    isFocusable = { it.isFocusable },
    visibleWhen = { item, state -> item.visibleWhen(state) },
    sectionOf = { it.section },
    sectionTitleRes = {
        when (it) {
            ButtonMappingItem.SECTION_CONTROLLER -> R.string.settings_button_mapping_section_controller
            ButtonMappingItem.SECTION_PRESETS -> R.string.settings_button_mapping_section_presets
            ButtonMappingItem.SECTION_KEYBOARD -> R.string.settings_button_mapping_section_keyboard
            else -> null
        }
    }
)

internal fun buttonMappingMaxFocusIndex(controls: ControlsState): Int = buttonMappingLayout.maxFocusIndex(controls)

internal fun buttonMappingItemAtFocusIndex(index: Int, controls: ControlsState): ButtonMappingItem? =
    buttonMappingLayout.itemAtFocusIndex(index, controls)

internal fun buttonMappingSections(controls: ControlsState) = buttonMappingLayout.buildSections(controls)

private fun ControlsState.targetKey(target: ButtonMappingTarget): String? = when (target) {
    ButtonMappingTarget.CONTROLLER -> buttonMappingController?.key
    ButtonMappingTarget.KEYBOARD -> NavigationButtonMaps.KEYBOARD
}

private fun ControlsState.standardCode(target: ButtonMappingTarget, action: NavigationAction): Int =
    action.standardCode(target == ButtonMappingTarget.KEYBOARD, swapAB, swapXY, swapStartSelect)

private fun ControlsState.mappedKey(target: ButtonMappingTarget, action: NavigationAction): Int? {
    val controller = targetKey(target) ?: return null
    val physical = navigationButtonMaps.physicalFor(controller, standardCode(target, action))
    return physical.takeUnless { target == ButtonMappingTarget.KEYBOARD && it in NavigationAction.GAMEPAD_BUTTON_CODES }
}

private fun ControlsState.defaultKey(target: ButtonMappingTarget, action: NavigationAction): Int? =
    standardCode(target, action)
        .takeUnless { target == ButtonMappingTarget.KEYBOARD && it in NavigationAction.GAMEPAD_BUTTON_CODES }

@Composable
private fun buttonMappingKeyName(keyCode: Int, target: ButtonMappingTarget): String =
    buttonMappingKeyLabelRes(keyCode, target == ButtonMappingTarget.KEYBOARD)?.let { stringResource(it) }
        ?: HotkeyManager.getKeyName(keyCode)

internal fun buttonMappingKeyLabelRes(keyCode: Int, keyboard: Boolean): Int? = when (keyCode) {
    KeyEvent.KEYCODE_DPAD_UP -> if (keyboard) R.string.settings_button_mapping_key_arrow_up else R.string.settings_button_mapping_key_dpad_up
    KeyEvent.KEYCODE_DPAD_DOWN -> if (keyboard) R.string.settings_button_mapping_key_arrow_down else R.string.settings_button_mapping_key_dpad_down
    KeyEvent.KEYCODE_DPAD_LEFT -> if (keyboard) R.string.settings_button_mapping_key_arrow_left else R.string.settings_button_mapping_key_dpad_left
    KeyEvent.KEYCODE_DPAD_RIGHT -> if (keyboard) R.string.settings_button_mapping_key_arrow_right else R.string.settings_button_mapping_key_dpad_right
    KeyEvent.KEYCODE_DPAD_CENTER -> R.string.settings_button_mapping_key_dpad_center
    KeyEvent.KEYCODE_ENTER -> R.string.settings_button_mapping_key_enter
    KeyEvent.KEYCODE_ESCAPE -> R.string.settings_button_mapping_key_escape
    KeyEvent.KEYCODE_SPACE -> R.string.settings_button_mapping_key_space
    KeyEvent.KEYCODE_TAB -> R.string.settings_button_mapping_key_tab
    KeyEvent.KEYCODE_DEL -> R.string.settings_button_mapping_key_backspace
    KeyEvent.KEYCODE_FORWARD_DEL -> R.string.settings_button_mapping_key_delete
    KeyEvent.KEYCODE_INSERT -> R.string.settings_button_mapping_key_insert
    KeyEvent.KEYCODE_MOVE_HOME -> R.string.settings_button_mapping_key_home
    KeyEvent.KEYCODE_MOVE_END -> R.string.settings_button_mapping_key_end
    KeyEvent.KEYCODE_PAGE_UP -> R.string.settings_button_mapping_key_page_up
    KeyEvent.KEYCODE_PAGE_DOWN -> R.string.settings_button_mapping_key_page_down
    KeyEvent.KEYCODE_SHIFT_LEFT -> R.string.settings_button_mapping_key_shift_left
    KeyEvent.KEYCODE_SHIFT_RIGHT -> R.string.settings_button_mapping_key_shift_right
    KeyEvent.KEYCODE_CTRL_LEFT -> R.string.settings_button_mapping_key_ctrl_left
    KeyEvent.KEYCODE_CTRL_RIGHT -> R.string.settings_button_mapping_key_ctrl_right
    KeyEvent.KEYCODE_ALT_LEFT -> R.string.settings_button_mapping_key_alt_left
    KeyEvent.KEYCODE_ALT_RIGHT -> R.string.settings_button_mapping_key_alt_right
    else -> null
}

internal fun navigationActionLabelRes(action: NavigationAction): Int = when (action) {
    NavigationAction.CONFIRM -> R.string.settings_button_mapping_action_confirm
    NavigationAction.BACK -> R.string.settings_button_mapping_action_back
    NavigationAction.CONTEXT -> R.string.settings_button_mapping_action_context
    NavigationAction.SECONDARY -> R.string.settings_button_mapping_action_secondary
    NavigationAction.MENU -> R.string.settings_button_mapping_action_menu
    NavigationAction.SELECT -> R.string.settings_button_mapping_action_select
    NavigationAction.PREV_SECTION -> R.string.settings_button_mapping_action_prev_section
    NavigationAction.NEXT_SECTION -> R.string.settings_button_mapping_action_next_section
    NavigationAction.PREV_TRIGGER -> R.string.settings_button_mapping_action_prev_trigger
    NavigationAction.NEXT_TRIGGER -> R.string.settings_button_mapping_action_next_trigger
    NavigationAction.LEFT_STICK_CLICK -> R.string.settings_button_mapping_action_left_stick
    NavigationAction.RIGHT_STICK_CLICK -> R.string.settings_button_mapping_action_right_stick
    NavigationAction.UP -> R.string.settings_button_mapping_action_up
    NavigationAction.DOWN -> R.string.settings_button_mapping_action_down
    NavigationAction.LEFT -> R.string.settings_button_mapping_action_left
    NavigationAction.RIGHT -> R.string.settings_button_mapping_action_right
}

@Composable
fun ButtonMappingSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val controls = uiState.controls
    val context = LocalContext.current
    val visibleItems = buttonMappingLayout.visibleItems(controls)
    val sections = buttonMappingLayout.buildSections(controls, context)

    fun isFocused(item: ButtonMappingItem): Boolean =
        uiState.focusedIndex == buttonMappingLayout.focusIndexOf(item, controls)

    SectionPaneLayout(
        items = visibleItems,
        sections = sections,
        focusedIndex = uiState.focusedIndex,
        focusToListIndex = { buttonMappingLayout.focusToListIndex(it, controls) },
        itemKey = { it.key },
        isNavItem = { it is ButtonMappingItem.SectionSpacer },
        isHeader = { it is ButtonMappingItem.Header },
        onSectionTap = { viewModel.setFocusIndex(it.focusStartIndex) },
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) { item ->
        when (item) {
            is ButtonMappingItem.Header -> Text(
                text = stringResource(item.titleRes).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = Dimens.spacingXs)
            )
            is ButtonMappingItem.SectionSpacer -> Spacer(modifier = Modifier.height(Dimens.spacingMd))

            ButtonMappingItem.ControllerInfo -> InfoPreference(
                title = stringResource(R.string.settings_button_mapping_controller_title),
                value = controls.buttonMappingController?.name
                    ?: stringResource(R.string.settings_button_mapping_controller_none),
                isFocused = isFocused(item)
            )

            is ButtonMappingItem.ActionRow -> {
                val mapped = controls.mappedKey(item.target, item.action)
                val default = controls.defaultKey(item.target, item.action)
                ActionPreference(
                    title = stringResource(navigationActionLabelRes(item.action)),
                    subtitle = if (default != null) {
                        stringResource(R.string.settings_button_mapping_default, buttonMappingKeyName(default, item.target))
                    } else {
                        stringResource(R.string.settings_button_mapping_default_none)
                    },
                    isFocused = isFocused(item),
                    trailingText = mapped?.let { buttonMappingKeyName(it, item.target) }
                        ?: stringResource(R.string.settings_button_mapping_unassigned),
                    onClick = { viewModel.startButtonCapture(item.target, item.action) }
                )
            }

            is ButtonMappingItem.Reset -> ActionPreference(
                title = stringResource(R.string.settings_button_mapping_reset_title),
                subtitle = stringResource(
                    when (item.target) {
                        ButtonMappingTarget.CONTROLLER -> R.string.settings_button_mapping_reset_controller_subtitle
                        ButtonMappingTarget.KEYBOARD -> R.string.settings_button_mapping_reset_keyboard_subtitle
                    }
                ),
                isFocused = isFocused(item),
                onClick = { viewModel.resetButtonMapping(item.target) }
            )

            ButtonMappingItem.SwapAB -> SwitchPreference(
                title = stringResource(R.string.settings_button_mapping_preset_swap_ab_title),
                subtitle = stringResource(R.string.settings_button_mapping_preset_swap_ab_subtitle),
                isEnabled = controls.swapAB,
                isFocused = isFocused(item),
                onToggle = { viewModel.setSwapAB(it) }
            )

            ButtonMappingItem.SwapXY -> SwitchPreference(
                title = stringResource(R.string.settings_button_mapping_preset_swap_xy_title),
                subtitle = stringResource(R.string.settings_button_mapping_preset_swap_xy_subtitle),
                isEnabled = controls.swapXY,
                isFocused = isFocused(item),
                onToggle = { viewModel.setSwapXY(it) }
            )

            ButtonMappingItem.SwapStartSelect -> SwitchPreference(
                title = stringResource(R.string.settings_button_mapping_preset_swap_start_select_title),
                subtitle = stringResource(R.string.settings_button_mapping_preset_swap_start_select_subtitle),
                isEnabled = controls.swapStartSelect,
                isFocused = isFocused(item),
                onToggle = { viewModel.setSwapStartSelect(it) }
            )
        }
    }

    controls.buttonMappingCapture?.let { capture ->
        ButtonCaptureModal(
            capture = capture,
            controllerName = controls.buttonMappingController?.name,
            onDismiss = { viewModel.cancelButtonCapture() }
        )
    }
}

@Composable
private fun ButtonCaptureModal(capture: ButtonMappingCapture, controllerName: String?, onDismiss: () -> Unit) {
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    LaunchedEffect(capture) {
        delay(CAPTURE_TIMEOUT_MS)
        currentOnDismiss.value()
    }
    val theme = LocalArgosyTheme.current
    Modal(
        title = stringResource(R.string.settings_button_mapping_capture_title),
        subtitle = stringResource(navigationActionLabelRes(capture.action)),
        baseWidth = Dimens.modalWidth,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusMd))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(Dimens.borderThin, theme.focusAccent, RoundedCornerShape(Dimens.radiusMd))
                .padding(Dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            Text(
                text = if (capture.target == ButtonMappingTarget.CONTROLLER && controllerName != null) {
                    stringResource(R.string.settings_button_mapping_capture_prompt_controller, controllerName)
                } else {
                    stringResource(R.string.settings_button_mapping_capture_prompt_keyboard)
                },
                style = MaterialTheme.typography.titleMedium,
                color = theme.focusAccent,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.settings_button_mapping_capture_cancel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private const val CAPTURE_TIMEOUT_MS = 10_000L
