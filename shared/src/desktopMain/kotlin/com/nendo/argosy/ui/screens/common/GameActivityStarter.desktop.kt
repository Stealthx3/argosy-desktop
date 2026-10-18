package com.nendo.argosy.ui.screens.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import javax.inject.Inject

actual class GameActivityStarter actual constructor(appContext: Context, activityContext: () -> Context?) {
    actual fun start(request: GameLaunchRequest): Result<Unit> = kotlin.Result.failure(UnsupportedOperationException("not available on desktop"))
}
