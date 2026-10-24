package com.nendo.argosy.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.nendo.argosy.data.model.SandboxPermission
import com.nendo.argosy.res.ArgosyR as R

val SandboxPermission.titleRes: Int
    get() = when (this) {
        SandboxPermission.GAMES_FOLDER -> R.string.settings_permissions_sandbox_games_folder_title
        SandboxPermission.EMULATOR_FOLDERS -> R.string.settings_permissions_sandbox_emulator_folders_title
        SandboxPermission.HOST_ACCESS -> R.string.settings_permissions_sandbox_host_title
        SandboxPermission.NOTIFICATIONS -> R.string.settings_permissions_sandbox_notifications_title
        SandboxPermission.SCREENSHOTS -> R.string.settings_permissions_sandbox_screenshots_title
        SandboxPermission.CONTROLLERS -> R.string.settings_permissions_sandbox_controllers_title
        SandboxPermission.NETWORK -> R.string.settings_permissions_sandbox_network_title
        SandboxPermission.AUDIO -> R.string.settings_permissions_sandbox_audio_title
        SandboxPermission.GPU -> R.string.settings_permissions_sandbox_gpu_title
        SandboxPermission.DISPLAY -> R.string.settings_permissions_sandbox_display_title
    }

val SandboxPermission.descriptionRes: Int
    get() = when (this) {
        SandboxPermission.GAMES_FOLDER -> R.string.settings_permissions_sandbox_games_folder_description
        SandboxPermission.EMULATOR_FOLDERS -> R.string.settings_permissions_sandbox_emulator_folders_description
        SandboxPermission.HOST_ACCESS -> R.string.settings_permissions_sandbox_host_description
        SandboxPermission.NOTIFICATIONS -> R.string.settings_permissions_sandbox_notifications_description
        SandboxPermission.SCREENSHOTS -> R.string.settings_permissions_sandbox_screenshots_description
        SandboxPermission.CONTROLLERS -> R.string.settings_permissions_sandbox_controllers_description
        SandboxPermission.NETWORK -> R.string.settings_permissions_sandbox_network_description
        SandboxPermission.AUDIO -> R.string.settings_permissions_sandbox_audio_description
        SandboxPermission.GPU -> R.string.settings_permissions_sandbox_gpu_description
        SandboxPermission.DISPLAY -> R.string.settings_permissions_sandbox_display_description
    }

val SandboxPermission.icon: ImageVector
    get() = when (this) {
        SandboxPermission.GAMES_FOLDER -> Icons.Default.Folder
        SandboxPermission.EMULATOR_FOLDERS -> Icons.Default.Save
        SandboxPermission.HOST_ACCESS -> Icons.Default.PlayArrow
        SandboxPermission.NOTIFICATIONS -> Icons.Default.Notifications
        SandboxPermission.SCREENSHOTS -> Icons.Default.ScreenshotMonitor
        SandboxPermission.CONTROLLERS -> Icons.Default.Gamepad
        SandboxPermission.NETWORK -> Icons.Default.Wifi
        SandboxPermission.AUDIO -> Icons.Default.VolumeUp
        SandboxPermission.GPU -> Icons.Default.Memory
        SandboxPermission.DISPLAY -> Icons.Default.DesktopWindows
    }

object FlatpakOverrideCommand {
    fun of(flags: List<String>): String =
        (listOf("flatpak", "override", "--user") + flags + SandboxPermission.APP_ID).joinToString(" ")
}
