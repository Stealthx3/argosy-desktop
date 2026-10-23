package com.nendo.argosy.ui.screens.common

import com.nendo.argosy.data.emulator.LinuxLaunch
import com.nendo.argosy.platform.HostGameProcess
import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.`package`

actual class GameActivityStarter actual constructor(appContext: Context, activityContext: () -> Context?) {
    actual fun start(request: GameLaunchRequest): Result<Unit> {
        val command = LinuxLaunch.commandOf(request.intent)
            ?: return Result.failure(UnsupportedOperationException("not available on desktop"))
        return HostGameProcess.start(request.intent.`package`, command, LinuxLaunch.directoryOf(request.intent))
    }
}
