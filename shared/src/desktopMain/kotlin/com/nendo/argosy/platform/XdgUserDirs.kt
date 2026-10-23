package com.nendo.argosy.platform

import java.io.File

/**
 * The host's XDG user directories: an absolute environment override,
 * then `user-dirs.dirs` (inside a Flatpak, the copy under the app's `XDG_CONFIG_HOME` first),
 * then the English default under the home folder.
 */
object XdgUserDirs {
    fun music(): File = resolve("MUSIC", "Music")

    fun download(): File = resolve("DOWNLOAD", "Downloads")

    private fun resolve(key: String, fallback: String): File {
        val home = System.getProperty("user.home")
        System.getenv("XDG_${key}_DIR")?.takeIf { it.startsWith("/") }?.let { return File(it) }
        val fromFile = configFiles(home).firstNotNullOfOrNull { file ->
            runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull()?.let { parse(it, home)[key] }
        }
        return File(fromFile ?: "$home/$fallback")
    }

    private fun configFiles(home: String): List<File> = buildList {
        System.getenv("XDG_CONFIG_HOME")?.takeIf { it.startsWith("/") }?.let { add(File(it, "user-dirs.dirs")) }
        add(File(home, ".config/user-dirs.dirs"))
    }.distinct()

    fun parse(text: String, home: String): Map<String, String> =
        text.lineSequence()
            .map { it.trim() }
            .mapNotNull { LINE.matchEntire(it) }
            .mapNotNull { match ->
                val raw = match.groupValues[2].replace(ESCAPE, "$1")
                val path = when {
                    raw == "\$HOME" -> home
                    raw.startsWith("\$HOME/") -> home + raw.removePrefix("\$HOME")
                    raw.startsWith("/") -> raw
                    else -> null
                }
                path?.trimEnd('/')?.ifEmpty { "/" }?.let { match.groupValues[1] to it }
            }
            .toMap()

    private val LINE = Regex("""XDG_([A-Z]+)_DIR\s*=\s*"((?:[^"\\]|\\.)*)"""")
    private val ESCAPE = Regex("""\\(.)""")
}
