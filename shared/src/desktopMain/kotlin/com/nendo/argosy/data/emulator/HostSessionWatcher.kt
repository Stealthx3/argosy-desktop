package com.nendo.argosy.data.emulator

import com.nendo.argosy.platform.HostGameProcess
import com.nendo.argosy.ui.screens.common.SessionEndCoordinator
import com.nendo.argosy.util.SafeCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The desktop's signal that a game closed: the host emulator process exiting ends its session
 * through [SessionEndCoordinator], as Argosy's window regaining focus does on Android, and
 * [HostSaveWatcher] watches the save folder while a session is open. At start
 * it recovers a session a previous run left open, as the Android application does at launch.
 */
@Singleton
class HostSessionWatcher @Inject constructor(
    private val sessionEndCoordinator: SessionEndCoordinator,
    private val playSessionTracker: PlaySessionTracker,
    private val hostSaveWatcher: HostSaveWatcher
) {
    private val scope = SafeCoroutineScope(Dispatchers.Main.immediate, "HostSessionWatcher")

    fun start() {
        scope.launch(Dispatchers.IO) { playSessionTracker.checkOrphanedSession() }
        scope.launch {
            playSessionTracker.activeSession.distinctUntilChangedBy { it?.gameId to it?.startTime }.collect { session ->
                if (session == null) hostSaveWatcher.stop() else hostSaveWatcher.start(session)
            }
        }
        scope.launch {
            var wasRunning = false
            HostGameProcess.running.map { it != null }.distinctUntilChanged().collect { running ->
                if (wasRunning && !running) {
                    withContext(Dispatchers.IO) { hostSaveWatcher.flush() }
                    sessionEndCoordinator.endClosedSession()
                }
                wasRunning = running
            }
        }
    }
}
