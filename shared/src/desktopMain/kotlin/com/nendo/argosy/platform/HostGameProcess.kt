package com.nendo.argosy.platform

import com.nendo.argosy.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one game process running on the host. On Android the emulator's activity covers Argosy and
 * stops it; here [running] is what the window's lifecycle follows until the process exits.
 */
object HostGameProcess {
    private const val TAG = "HostGameProcess"

    data class Session(val packageName: String?, val command: List<String>)

    private val _running = MutableStateFlow<Session?>(null)
    val running: StateFlow<Session?> = _running.asStateFlow()

    @Volatile
    private var process: Process? = null

    @Synchronized
    fun start(packageName: String?, command: List<String>, workingDirectory: String? = null): Result<Unit> {
        if (process?.isAlive == true) {
            return Result.failure(IllegalStateException("A game is already running"))
        }
        val log = DesktopDirectories.data.resolve("logs").apply { mkdirs() }.resolve("emulator.log")
        return try {
            val started = ProcessBuilder(HostShell.hostCommand(command, workingDirectory))
                .apply { if (workingDirectory != null && !HostShell.isSandboxed) directory(java.io.File(workingDirectory)) }
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.to(log))
                .start()
            process = started
            _running.value = Session(packageName, command)
            Logger.info(TAG, "Started ${command.first()} for $packageName (pid ${started.pid()})")
            started.onExit().thenAccept { exited ->
                Logger.info(TAG, "Game process exited with ${exited.exitValue()}")
                finish(exited)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.error(TAG, "Could not start ${command.first()}", e)
            Result.failure(e)
        }
    }

    @Synchronized
    fun stop(packageName: String) {
        if (_running.value?.packageName != packageName) return
        process?.let { running ->
            running.descendants().forEach { it.destroy() }
            running.destroy()
        }
    }

    @Synchronized
    private fun finish(exited: Process) {
        if (process !== exited) return
        process = null
        _running.value = null
    }
}
