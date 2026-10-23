package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.emulator.savepath.SavePathAuthority
import com.nendo.argosy.data.emulator.savepath.SavePathRequest
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.repository.SaveSyncRepository
import com.nendo.argosy.util.Logger
import com.nendo.argosy.util.SafeCoroutineScope
import java.io.File
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

/**
 * The desktop's counterpart of the file watcher in Android's GameSessionService: watches the
 * session's save folder while a host emulator runs and reports each write to
 * [PlaySessionTracker.reportSaveWritten], so a short session that saved is still synced. The save
 * path, the startup cooldown and the ignored names and folders are Android's. Android's live save
 * caching on each write is not ported; the session-end sync captures the save.
 */
@Singleton
class HostSaveWatcher @Inject constructor(
    private val gameDao: GameDao,
    private val emulatorResolver: EmulatorResolver,
    private val savePathAuthority: SavePathAuthority,
    private val saveSyncRepository: dagger.Lazy<SaveSyncRepository>,
    private val playSessionTracker: PlaySessionTracker
) {
    private val scope = SafeCoroutineScope(Dispatchers.IO, "HostSaveWatcher")
    private var job: Job? = null
    private var watchService: WatchService? = null
    private var sessionStart: Instant = Instant.EPOCH

    @Synchronized
    fun start(session: ActiveSession) {
        stop()
        sessionStart = session.startTime
        job = scope.launch {
            val watchDir = resolveWatchDir(session) ?: run {
                Logger.debug(TAG, "[GameSession] No save path to watch for gameId=${session.gameId}")
                return@launch
            }
            while (isActive && !watchDir.isDirectory) delay(POLL_INTERVAL_MS)
            val service = FileSystems.getDefault().newWatchService()
            synchronized(this@HostSaveWatcher) {
                if (!isActive) {
                    service.close()
                    return@launch
                }
                watchService = service
            }
            val dirs = watchedDirs(watchDir)
            val keys = dirs.associateBy { dir ->
                dir.toPath().register(
                    service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                )
            }
            Logger.debug(TAG, "[GameSession] Watching ${dirs.size} save folders under $watchDir for gameId=${session.gameId}")
            try {
                while (isActive) {
                    val key = runInterruptible { service.take() }
                    handleEvents(key, keys)
                }
            } catch (_: ClosedWatchServiceException) {
            }
        }
    }

    /**
     * Reports the writes still queued when the emulator exits, so a save written on close counts
     * before the session end decides whether the session saved.
     */
    @Synchronized
    fun flush() {
        val service = watchService ?: return
        try {
            while (true) {
                val key = service.poll(FLUSH_WAIT_MS, TimeUnit.MILLISECONDS) ?: break
                handleEvents(key, null)
            }
        } catch (_: ClosedWatchServiceException) {
        }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        try {
            watchService?.close()
        } catch (_: Exception) {
        }
        watchService = null
    }

    private fun handleEvents(key: WatchKey, keys: Map<WatchKey, File>?) {
        val names = key.pollEvents().mapNotNull { (it.context() as? Path)?.toString() }
        key.reset()
        val written = names.filterNot { it.startsWith(".") || it.endsWith(".tmp") || it.endsWith(".bak") }
        if (written.isEmpty()) return
        val elapsed = Duration.between(sessionStart, Instant.now()).toMillis()
        if (elapsed < STARTUP_COOLDOWN_MS) {
            Logger.debug(TAG, "[GameSession] Ignoring early save event (${elapsed}ms): $written")
            return
        }
        Logger.debug(TAG, "[GameSession] Save change detected: $written in ${keys?.get(key)?.name ?: "save folder"}")
        playSessionTracker.reportSaveWritten()
    }

    private suspend fun resolveWatchDir(session: ActiveSession): File? {
        if (session.variantFileId != null) return null
        val game = gameDao.getById(session.gameId) ?: return null
        val emulatorId = emulatorResolver.resolveEmulatorId(session.emulatorPackage) ?: return null
        val request = SavePathRequest(
            platformSlug = game.platformSlug,
            emulatorId = emulatorId,
            emulatorPackage = session.emulatorPackage
        )
        if (savePathAuthority.configFor(request) == null) return null
        val repository = saveSyncRepository.get()
        val savePath = repository.discoverSavePath(
            emulatorId = emulatorId,
            gameTitle = game.title,
            platformSlug = game.platformSlug,
            romPath = game.localPath,
            cachedSaveId = game.saveId ?: game.titleId,
            coreName = session.coreName,
            emulatorPackage = session.emulatorPackage,
            gameId = session.gameId
        ) ?: repository.predictFolderSavePath(
            emulatorId = emulatorId,
            platformSlug = game.platformSlug,
            romPath = game.localPath,
            gameId = session.gameId,
            gameTitle = game.title,
            cachedSaveId = game.saveId ?: game.titleId,
            emulatorPackage = session.emulatorPackage
        ) ?: return null
        val file = File(savePath)
        return if (file.isDirectory) file else file.parentFile ?: file
    }

    private fun watchedDirs(watchDir: File): List<File> =
        listOf(watchDir) + watchDir.walkTopDown().maxDepth(3)
            .filter { it.isDirectory && it != watchDir }
            .filterNot { dir -> IGNORED_DIRECTORY_PATTERNS.any { dir.name.lowercase().contains(it) } }

    private companion object {
        const val TAG = "HostSaveWatcher"
        const val POLL_INTERVAL_MS = 2000L
        const val STARTUP_COOLDOWN_MS = 20000L
        const val FLUSH_WAIT_MS = 300L
        val IGNORED_DIRECTORY_PATTERNS = setOf("cache", "shader", "shaders", "gpu_cache", "temp", "log", "logs")
    }
}
