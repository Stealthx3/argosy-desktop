// Extracted verbatim from app/src/main/kotlin/com/nendo/argosy/ui/screens/common/GameLaunchDelegate.kt: DiscPickerState SyncOverlayState MemcardPickerState VariantPickerState
package com.nendo.argosy.ui.screens.common

import android.content.Intent
import com.nendo.argosy.data.emulator.DiscOption
import com.nendo.argosy.data.emulator.LaunchOrigin
import com.nendo.argosy.domain.model.SyncProgress
import com.nendo.argosy.domain.model.SyncState
import com.nendo.argosy.libretro.LaunchMode

data class DiscPickerState(
    val gameId: Long,
    val discs: List<DiscOption>,
    val channelName: String? = null,
    val launchMode: LaunchMode? = null,
    val origin: LaunchOrigin = LaunchOrigin.INTERNAL,
    val onLaunch: (Intent) -> Unit
)

data class SyncOverlayState(
    val gameTitle: String,
    val syncProgress: SyncProgress,
    @Deprecated("Use syncProgress instead")
    val syncState: SyncState = SyncState.Idle,
    val onGrantPermission: (() -> Unit)? = null,
    val onDisableSync: (() -> Unit)? = null,
    val onOpenSettings: (() -> Unit)? = null,
    val onSkip: (() -> Unit)? = null
)

data class MemcardPickerState(
    val gameId: Long,
    val emulatorId: String,
    val platformName: String,
    val cards: List<com.nendo.argosy.data.sync.platform.MemcardInfo>,
    val channelName: String? = null,
    val launchMode: LaunchMode? = null,
    val origin: LaunchOrigin = LaunchOrigin.INTERNAL,
    val onLaunch: (Intent) -> Unit
)

data class VariantPickerState(
    val gameId: Long,
    val variants: List<com.nendo.argosy.data.emulator.VariantOption>,
    val channelName: String? = null,
    val launchMode: LaunchMode? = null,
    val origin: LaunchOrigin = LaunchOrigin.INTERNAL,
    val onLaunch: (Intent) -> Unit
)
