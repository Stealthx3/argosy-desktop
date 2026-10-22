package com.nendo.argosy.platform

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Runs commands on the host system. Inside the Flatpak sandbox every command goes through
 * `flatpak-spawn --host`, so detection and launch see the host's apps, PATH and home.
 */
object HostShell {
    private const val TIMEOUT_SECONDS = 15L

    val isSandboxed: Boolean by lazy { File("/.flatpak-info").exists() }

    fun hostCommand(args: List<String>): List<String> =
        if (isSandboxed) listOf("flatpak-spawn", "--host") + args else args

    fun runScript(script: String, args: List<String> = emptyList()): String? =
        run(listOf("sh", "-c", script, "sh") + args)

    fun run(args: List<String>): String? = try {
        val process = ProcessBuilder(hostCommand(args))
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) output else {
            process.destroyForcibly()
            null
        }
    } catch (_: Exception) {
        null
    }
}
