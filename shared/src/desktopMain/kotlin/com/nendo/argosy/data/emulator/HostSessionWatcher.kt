package com.nendo.argosy.data.emulator

import com.nendo.argosy.platform.HostGameProcess
import com.nendo.argosy.ui.screens.common.SessionEndCoordinator
import com.nendo.argosy.util.SafeCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The desktop's signal that a game closed: the host emulator process exiting ends its session
 * through [SessionEndCoordinator], as Argosy's window regaining focus does on Android. At start
 * it recovers a session a previous run left open, as the Android application does at launch.
 */
@Singleton
class HostSessionWatcher @Inject constructor(
    private val sessionEndCoordinator: SessionEndCoordinator,
    private val playSessionTracker: PlaySessionTracker
) {
    private val scope = SafeCoroutineScope(Dispatchers.Main.immediate, "HostSessionWatcher")

    fun start() {
        scope.launch(Dispatchers.IO) { playSessionTracker.checkOrphanedSession() }
        scope.launch {
            var wasRunning = false
            HostGameProcess.running.map { it != null }.distinctUntilChanged().collect { running ->
                if (wasRunning && !running) sessionEndCoordinator.endClosedSession()
                wasRunning = running
            }
        }
    }
}
