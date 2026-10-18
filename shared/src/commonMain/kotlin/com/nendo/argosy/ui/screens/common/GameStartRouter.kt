package com.nendo.argosy.ui.screens.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.className
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.PlatformIntent
import com.nendo.argosy.DualScreenManagerHolder
import com.nendo.argosy.util.Logger
import com.nendo.argosy.util.SafeCoroutineScope
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GameStartRouter"

/**
 * Process-lifetime entry point that resolves a game's placement and starts it.
 */
@Singleton
class GameStartRouter internal constructor(
    context: Context,
    private val launchTargetResolver: EmulatorLaunchTargetResolver,
    private val scope: CoroutineScope
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        launchTargetResolver: EmulatorLaunchTargetResolver
    ) : this(context, launchTargetResolver, SafeCoroutineScope(Dispatchers.Main.immediate, "GameStart"))

    private val fallbackStarter = GameActivityStarter(context) { null }

    fun start(
        gameId: Long,
        intent: Intent,
        overrideDisplayId: Int? = null,
        onFailure: ((Throwable) -> Unit)? = null
    ) {
        scope.launch {
            val result = try {
                val options = launchTargetResolver.launchOptionsFor(gameId, overrideDisplayId)
                startResolved(GameLaunchRequest(intent, options))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, "Placement for gameId=$gameId could not be resolved", e)
                Result.failure(e)
            }
            result.exceptionOrNull()?.let { onFailure?.invoke(it) }
        }
    }

    private fun startResolved(request: GameLaunchRequest): Result<Unit> {
        val manager = DualScreenManagerHolder.instance ?: return fallbackStarter.start(request)
        return manager.startGame(request)
    }
}
