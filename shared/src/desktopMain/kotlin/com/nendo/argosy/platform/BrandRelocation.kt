package com.nendo.argosy.platform

import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.nendo.argosy.util.Logger
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

class BrandRelocation(
    private val home: File,
    private val dataHome: File,
    private val cacheHome: File,
    private val sandboxed: Boolean,
    private val runHostScript: (String, List<String>) -> String?,
) {
    fun run() {
        val data = dataHome.resolve(APP_FOLDER)
        val cache = cacheHome.resolve(APP_FOLDER)
        val moves = mutableListOf<PathMove>()
        var movedSomething = false
        for ((kind, target) in listOf("data" to data, "cache" to cache)) {
            val base = if (kind == "data") dataHome else cacheHome
            val sameBase = base.resolve(PREVIOUS_APP_FOLDER)
            moves += PathMove(sameBase.path, target.path)
            if (!target.exists() && sameBase.isDirectory && sameBase.renameTo(target)) movedSomething = true
            if (sandboxed) {
                for (previous in PREVIOUS_FLATPAKS) {
                    val oldFlatpak = home.resolve(".var/app/${previous.appId}/$kind/${previous.folder}")
                    moves += PathMove(oldFlatpak.path, target.path)
                    if (!target.exists() && runHostScript(MOVE_FOLDER, listOf(oldFlatpak.path, target.path)).orEmpty().contains("moved")) {
                        Logger.info(TAG, "moved the $kind folder of ${previous.appId}")
                        movedSomething = true
                    }
                }
            }
        }
        val oldLibrary = home.resolve(PREVIOUS_LIBRARY)
        val library = home.resolve(LIBRARY)
        val merge = runHostScript(MERGE_LIBRARY, listOf(oldLibrary.path, library.path)).orEmpty()
        if ("merged" in merge) {
            moves += PathMove(oldLibrary.path, library.path)
            movedSomething = true
            merge.lineSequence().filter { it.startsWith("kept:") || it.startsWith("failed:") }
                .forEach { Logger.warn(TAG, "library move left ${it.replace(home.path, "~")}") }
            Logger.info(TAG, "merged the previous library folder (${if ("removed" in merge) "old folder removed" else "old folder kept"})")
        } else if ("other-device" in merge) {
            Logger.warn(TAG, "previous library folder is on another device; left in place")
        }
        val marker = data.resolve(MARKER)
        val markerVersion = runCatching { marker.readText().trim() }.getOrNull()
        if (!movedSomething && markerVersion == MARKER_VERSION) return
        if (!data.isDirectory) return
        rewriteStoredPaths(data, moves)
        if (markerVersion != null && markerVersion != MARKER_VERSION) {
            runCatching { data.resolve(STEAM_SHORTCUT_STALE).writeText("") }
        }
        runCatching { marker.writeText(MARKER_VERSION) }
    }

    private fun rewriteStoredPaths(data: File, moves: List<PathMove>) {
        val rewriter = PathRewriter(moves)
        runCatching { rewriteDatabase(data.resolve("alauncher.db"), rewriter) }
            .onFailure { Logger.error(TAG, "database path rewrite failed", it) }
        data.resolve("datastore").listFiles { f -> f.name.endsWith(".preferences_pb") }.orEmpty().forEach { file ->
            runCatching { rewritePreferences(file, rewriter) }
                .onFailure { Logger.error(TAG, "preferences path rewrite failed: ${file.name}", it) }
        }
        data.resolve("files/shared_prefs").listFiles { f -> f.name.endsWith(".properties") }.orEmpty().forEach { file ->
            runCatching { rewriteProperties(file, rewriter) }
                .onFailure { Logger.error(TAG, "properties path rewrite failed: ${file.name}", it) }
        }
    }

    companion object {
        private const val TAG = "BrandRelocation"
        const val APP_FOLDER = "icebreaker"
        const val PREVIOUS_APP_FOLDER = "argosy-desktop"
        val PREVIOUS_FLATPAKS = listOf(
            PreviousFlatpak("io.github.stealthx3.IceBreaker", APP_FOLDER),
            PreviousFlatpak("io.github.stealthx3.ArgosyDesktop", PREVIOUS_APP_FOLDER),
        )
        const val LIBRARY = "Games/IceBreaker"
        const val PREVIOUS_LIBRARY = "Games/Argosy"
        private const val MARKER = ".brand-relocation"
        private const val MARKER_VERSION = "2"
        const val STEAM_SHORTCUT_STALE = ".steam-shortcut-stale"

        private val MOVE_FOLDER = """
            [ -d "$1" ] && [ ! -L "$1" ] && [ ! -e "$2" ] || exit 0
            mkdir -p "${'$'}{2%/*}" && mv -T "$1" "$2" && echo moved
        """.trimIndent()

        private val MERGE_LIBRARY = """
            merge() {
              local e n t
              for e in "$1"/* "$1"/.[!.]* "$1"/..?*; do
                [ -e "${'$'}e" ] || [ -L "${'$'}e" ] || continue
                n=${'$'}{e##*/}
                t="$2/${'$'}n"
                if [ ! -e "${'$'}t" ] && [ ! -L "${'$'}t" ]; then
                  mv -T "${'$'}e" "${'$'}t" || echo "failed:${'$'}e"
                elif [ -d "${'$'}e" ] && [ ! -L "${'$'}e" ] && [ -d "${'$'}t" ] && [ ! -L "${'$'}t" ]; then
                  merge "${'$'}e" "${'$'}t"
                  rmdir "${'$'}e" 2>/dev/null
                else
                  echo "kept:${'$'}e"
                fi
              done
            }
            [ -d "$1" ] && [ ! -L "$1" ] || exit 0
            mkdir -p "$2" || exit 0
            [ "${'$'}(stat -c %d "$1")" = "${'$'}(stat -c %d "$2")" ] || { echo other-device; exit 0; }
            merge "$1" "$2"
            rmdir "$1" 2>/dev/null && echo removed
            echo merged
        """.trimIndent()

        private fun xdg(variable: String, fallback: String): File {
            val home = System.getProperty("user.home")
            return File(System.getenv(variable)?.takeIf { it.isNotBlank() } ?: (home + fallback))
        }

        fun steamShortcutStaleFlag(): File = xdg("XDG_DATA_HOME", "/.local/share").resolve(APP_FOLDER).resolve(STEAM_SHORTCUT_STALE)

        fun forThisMachine(): BrandRelocation {
            val home = File(System.getProperty("user.home"))
            return BrandRelocation(
                home = home,
                dataHome = xdg("XDG_DATA_HOME", "/.local/share"),
                cacheHome = xdg("XDG_CACHE_HOME", "/.cache"),
                sandboxed = HostShell.isSandboxed,
                runHostScript = HostShell::runScript,
            )
        }
    }
}

data class PathMove(val from: String, val to: String)

data class PreviousFlatpak(val appId: String, val folder: String)

class PathRewriter(moves: List<PathMove>) {
    private val rules = moves.filter { it.from != it.to }.distinct().map { move ->
        Regex("(?<![^\"'=:,;\\[({\\s])" + Regex.escape(move.from) + "(?=/|$|[\"',;)\\]}\\s])") to move.to
    }

    val needles: List<String> = moves.filter { it.from != it.to }.map { it.from }.distinct()

    fun rewrite(value: String): String =
        if (needles.none { it in value }) value
        else rules.fold(value) { acc, (pattern, to) -> pattern.replace(acc, Regex.escapeReplacement(to)) }
}

internal fun rewriteDatabase(file: File, rewriter: PathRewriter): Int {
    if (!file.isFile || rewriter.needles.isEmpty()) return 0
    val connection = BundledSQLiteDriver().open(file.absolutePath)
    var changed = 0
    try {
        val backup = file.resolveSibling(file.name + ".before-relocation")
        if (!backup.exists()) {
            connection.prepare("VACUUM INTO ?").use { it.bindText(1, backup.absolutePath); it.step() }
        }
        connection.execSQL("BEGIN IMMEDIATE")
        for (table in rewritableTables(connection)) {
            for (column in textColumns(connection, table)) {
                changed += rewriteColumn(connection, table, column, rewriter)
            }
        }
        connection.execSQL("COMMIT")
    } catch (e: Exception) {
        runCatching { connection.execSQL("ROLLBACK") }
        throw e
    } finally {
        connection.close()
    }
    return changed
}

private fun rewritableTables(connection: SQLiteConnection): List<String> {
    val tables = mutableListOf<Pair<String, String>>()
    connection.prepare("SELECT name, COALESCE(sql, '') FROM sqlite_master WHERE type = 'table'").use { stmt ->
        while (stmt.step()) tables += stmt.getText(0) to stmt.getText(1)
    }
    val virtual = tables.filter { it.second.trimStart().startsWith("CREATE VIRTUAL TABLE", ignoreCase = true) }.map { it.first }
    return tables.map { it.first }.filterNot { name ->
        name.startsWith("sqlite_") || name == "room_master_table" || name == "android_metadata" ||
            name in virtual || virtual.any { name.startsWith(it + "_") }
    }
}

private fun textColumns(connection: SQLiteConnection, table: String): List<String> =
    connection.prepare("PRAGMA table_info(`$table`)").use { stmt ->
        buildList { while (stmt.step()) add(stmt.getText(1)) }
    }

private fun rewriteColumn(connection: SQLiteConnection, table: String, column: String, rewriter: PathRewriter): Int {
    val where = rewriter.needles.joinToString(" OR ") { "instr(`$column`, ?) > 0" }
    val rows = mutableListOf<Pair<Long, String>>()
    connection.prepare("SELECT rowid, `$column` FROM `$table` WHERE typeof(`$column`) = 'text' AND ($where)").use { stmt ->
        rewriter.needles.forEachIndexed { i, needle -> stmt.bindText(i + 1, needle) }
        while (stmt.step()) rows += stmt.getLong(0) to stmt.getText(1)
    }
    var changed = 0
    for ((rowid, value) in rows) {
        val updated = rewriter.rewrite(value)
        if (updated == value) continue
        runCatching {
            connection.prepare("UPDATE `$table` SET `$column` = ? WHERE rowid = ?").use { stmt ->
                stmt.bindText(1, updated)
                stmt.bindLong(2, rowid)
                stmt.step()
            }
            changed++
        }.onFailure { Logger.warn("BrandRelocation", "kept $table.$column row $rowid: ${it.message}") }
    }
    return changed
}

internal fun rewritePreferences(file: File, rewriter: PathRewriter): Boolean = runBlocking {
    val prefs = file.source().buffer().use { PreferencesSerializer.readFrom(it) }
    val edited = prefs.toMutablePreferences()
    var changed = false
    for ((key, value) in prefs.asMap()) {
        when (value) {
            is String -> rewriter.rewrite(value).takeIf { it != value }?.let {
                edited[stringPreferencesKey(key.name)] = it
                changed = true
            }
            is Set<*> -> {
                val strings = value.filterIsInstance<String>()
                if (strings.size == value.size) {
                    val updated = strings.map(rewriter::rewrite).toSet()
                    if (updated != value) {
                        edited[stringSetPreferencesKey(key.name)] = updated
                        changed = true
                    }
                }
            }
        }
    }
    if (changed) {
        val tmp = file.resolveSibling(file.name + ".relocating")
        tmp.sink().buffer().use { PreferencesSerializer.writeTo(edited, it) }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
    changed
}

internal fun rewriteProperties(file: File, rewriter: PathRewriter): Boolean {
    val properties = Properties().apply { file.inputStream().use(::load) }
    var changed = false
    for (name in properties.stringPropertyNames()) {
        val value = properties.getProperty(name)
        val updated = rewriter.rewrite(value)
        if (updated != value) {
            properties.setProperty(name, updated)
            changed = true
        }
    }
    if (changed) {
        val tmp = file.resolveSibling(file.name + ".relocating")
        tmp.outputStream().use { properties.store(it, null) }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
    return changed
}
