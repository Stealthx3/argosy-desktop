package com.nendo.argosy.data.emulator

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.PlatformIntent

data class DiscOption(
    val fileName: String,
    val filePath: String,
    val discNumber: Int
)

sealed class LaunchResult {
    /**
     * @property variantFileId the `game_files` row that launched in place of the primary rom; null
     * when the primary rom launched, whichever variant the caller requested.
     */
    data class Success(
        val intent: Intent,
        val discId: Long? = null,
        val alreadyLaunched: Boolean = false,
        val inProcess: Boolean = false,
        val variantFileId: Long? = null
    ) : LaunchResult()
    data class SelectDisc(val gameId: Long, val discs: List<DiscOption>) : LaunchResult()
    data class SelectVariant(val gameId: Long, val variants: List<VariantOption>) : LaunchResult()
    data class SelectMemcard(
        val gameId: Long,
        val emulatorId: String,
        val platformName: String,
        val cards: List<com.nendo.argosy.data.sync.platform.MemcardInfo>
    ) : LaunchResult()
    data class NoEmulator(val platformSlug: String) : LaunchResult()
    data class NoRomFile(val gamePath: String?) : LaunchResult()
    data class NoSteamLauncher(val launcherPackage: String) : LaunchResult()
    data class NoAndroidApp(val packageName: String) : LaunchResult()
    data class NoCore(val platformSlug: String, val reason: String? = null) : LaunchResult()
    data class MissingDiscs(val missingDiscNumbers: List<Int>) : LaunchResult()
    data class MissingBios(val platformSlug: String) : LaunchResult()
    data class NoScummVMGameId(val gameName: String) : LaunchResult()
    data class Error(val message: String) : LaunchResult()
    data object Cancelled : LaunchResult()
}
