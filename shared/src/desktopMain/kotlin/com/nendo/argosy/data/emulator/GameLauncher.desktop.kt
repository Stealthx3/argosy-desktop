package com.nendo.argosy.data.emulator

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.PlatformIntent
import com.nendo.argosy.data.local.entity.GameEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class GameLauncher @Inject constructor() {
    actual suspend fun launch(gameId: Long, discId: Long?, forResume: Boolean, selectedDiscPath: String?, variantFileId: Long?, skipVariantPrompt: Boolean, allowVariantPrompt: Boolean, prefetchedGame: GameEntity?): LaunchResult = TODO("desktop: launch")

    actual suspend fun buildInstallIntent(game: GameEntity, file: File): Intent? = null

    actual suspend fun buildBatchInstallIntent(game: GameEntity, files: List<File>): Intent? = null
}
