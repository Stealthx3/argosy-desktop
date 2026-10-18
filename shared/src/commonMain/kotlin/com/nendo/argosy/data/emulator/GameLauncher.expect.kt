package com.nendo.argosy.data.emulator

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.className
import com.nendo.argosy.platform.PlatformIntent
import com.nendo.argosy.data.local.entity.GameEntity
import java.io.File

expect class GameLauncher {
    suspend fun launch(gameId: Long, discId: Long? = null, forResume: Boolean = false, selectedDiscPath: String? = null, variantFileId: Long? = null, skipVariantPrompt: Boolean = false, allowVariantPrompt: Boolean = true, prefetchedGame: GameEntity? = null): LaunchResult

    suspend fun forceStopEmulator(packageName: String)

    suspend fun buildInstallIntent(game: GameEntity, file: File): Intent?

    suspend fun buildBatchInstallIntent(game: GameEntity, files: List<File>): Intent?
}
