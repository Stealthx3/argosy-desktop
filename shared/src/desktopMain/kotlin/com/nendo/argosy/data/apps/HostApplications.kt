package com.nendo.argosy.data.apps

import com.nendo.argosy.platform.HostShell
import java.util.Locale

data class HostApplication(
    val id: String,
    val name: String,
    val exec: List<String>,
    val workingDirectory: String?,
    val icon: String?,
    val path: String,
    val isSystem: Boolean,
    val isGame: Boolean
)

/**
 * The host's launchable applications, read from the desktop entries of the XDG data directories
 * and the Flatpak exports, with the Desktop Entry Specification's precedence and visibility rules.
 * Every read goes through [HostShell]: inside the sandbox it sees the host's entries.
 */
object HostApplications {
    private const val BASE_MARK = "@@BASE "
    private const val FILE_MARK = "@@FILE "
    private const val DESKTOP_MARK = "@@CURRENT "

    private val SCAN_SCRIPT = """
        echo "$DESKTOP_MARK${'$'}{XDG_CURRENT_DESKTOP}"
        printf '%s\n' "${'$'}{XDG_DATA_HOME:-${'$'}HOME/.local/share}" | tr ':' '\n'
        printf '%s\n' "${'$'}{XDG_DATA_DIRS:-/usr/local/share:/usr/share}" | tr ':' '\n'
        echo "${'$'}HOME/.local/share/flatpak/exports/share"
        echo /var/lib/flatpak/exports/share
    """.trimIndent()

    private val READ_SCRIPT = """
        for d in "${'$'}@"; do
          [ -d "${'$'}d/applications" ] || continue
          echo "$BASE_MARK${'$'}d/applications"
          find -L "${'$'}d/applications" -name '*.desktop' -type f -print0 2>/dev/null | sort -z |
            xargs -0 -r awk 'FNR==1{print "$FILE_MARK" FILENAME; g=0} /^\[/{g=(${'$'}0=="[Desktop Entry]"); next} g'
        done
    """.trimIndent()

    private val TRY_EXEC_SCRIPT = """
        for t in "${'$'}@"; do
          case "${'$'}t" in /*) [ -x "${'$'}t" ] || echo "${'$'}t" ;; *) command -v "${'$'}t" >/dev/null 2>&1 || echo "${'$'}t" ;; esac
        done
    """.trimIndent()

    private val FINGERPRINT_SCRIPT = """
        for d in "${'$'}@"; do stat -c '%n %Y' "${'$'}d/applications" 2>/dev/null; done
    """.trimIndent()

    @Volatile
    private var cached: Map<String, HostApplication> = emptyMap()

    @Volatile
    private var dataDirs: List<String> = emptyList()

    fun all(): List<HostApplication> {
        val (desktops, dirs) = locate() ?: return emptyList()
        val output = HostShell.runScript(READ_SCRIPT, dirs) ?: return emptyList()
        val candidates = parseEntries(output, userDataHome = dirs.first())
        val missingTryExec = candidates.mapNotNull { it.tryExec }.distinct().let { tryExecs ->
            if (tryExecs.isEmpty()) emptySet()
            else HostShell.runScript(TRY_EXEC_SCRIPT, tryExecs)?.lines()?.filter { it.isNotBlank() }?.toSet()
                ?: emptySet()
        }
        val visible = selectVisible(candidates, desktops, missingTryExec, Locale.getDefault())
        cached = visible.associateBy { it.id }
        return visible
    }

    fun find(id: String): HostApplication? = cached.ifEmpty { all().associateBy { it.id } }[id]

    fun dataDirectories(): List<String> = dataDirs.ifEmpty { locate()?.second.orEmpty() }

    fun fingerprint(): String? {
        val dirs = dataDirectories().ifEmpty { return null }
        return HostShell.runScript(FINGERPRINT_SCRIPT, dirs)
    }

    private fun locate(): Pair<List<String>, List<String>>? {
        val lines = HostShell.runScript(SCAN_SCRIPT)?.lines() ?: return null
        val desktops = lines.firstOrNull { it.startsWith(DESKTOP_MARK) }
            ?.removePrefix(DESKTOP_MARK)
            ?.split(':')
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val dirs = lines.filter { it.startsWith("/") }.map { it.trimEnd('/') }.distinct()
        dataDirs = dirs
        return desktops to dirs
    }

    internal data class Candidate(
        val id: String,
        val path: String,
        val isSystem: Boolean,
        val keys: Map<String, String>
    ) {
        val tryExec: String? get() = keys["TryExec"]?.takeIf { it.isNotBlank() }
    }

    internal fun parseEntries(output: String, userDataHome: String): List<Candidate> {
        val candidates = mutableListOf<Candidate>()
        var base: String? = null
        var path: String? = null
        var keys = LinkedHashMap<String, String>()

        fun flush() {
            val file = path ?: return
            val root = base ?: return
            val id = file.removePrefix("$root/").removeSuffix(".desktop").replace('/', '-')
            candidates += Candidate(id, file, isSystemRoot(root, userDataHome), keys)
        }

        for (line in output.lineSequence()) {
            when {
                line.startsWith(BASE_MARK) -> {
                    flush()
                    path = null
                    base = line.removePrefix(BASE_MARK)
                }
                line.startsWith(FILE_MARK) -> {
                    flush()
                    path = line.removePrefix(FILE_MARK)
                    keys = LinkedHashMap()
                }
                line.isBlank() || line.startsWith("#") -> Unit
                else -> {
                    val separator = line.indexOf('=')
                    if (separator > 0) {
                        val key = line.substring(0, separator).trim()
                        if (key !in keys) keys[key] = line.substring(separator + 1).trim()
                    }
                }
            }
        }
        flush()
        return candidates
    }

    internal fun selectVisible(
        candidates: List<Candidate>,
        currentDesktops: List<String>,
        missingTryExec: Set<String>,
        locale: Locale
    ): List<HostApplication> = candidates
        .distinctBy { it.id }
        .filter { it.isShown(currentDesktops, missingTryExec) }
        .mapNotNull { it.toApplication(locale) }

    private fun Candidate.isShown(currentDesktops: List<String>, missingTryExec: Set<String>): Boolean {
        if (keys["Type"] != "Application") return false
        if (keys.isTrue("Hidden") || keys.isTrue("NoDisplay") || keys.isTrue("Terminal")) return false
        tryExec?.let { if (it in missingTryExec) return false }
        val onlyShowIn = keys.list("OnlyShowIn")
        if (onlyShowIn.isNotEmpty() && onlyShowIn.none { it in currentDesktops }) return false
        if (keys.list("NotShowIn").any { it in currentDesktops }) return false
        return true
    }

    private fun Candidate.toApplication(locale: Locale): HostApplication? {
        val name = localized("Name", locale) ?: return null
        val icon = keys["Icon"]?.let(DesktopEntryValues::unescape)?.takeIf { it.isNotBlank() }
        val exec = keys["Exec"]?.let { DesktopEntryValues.execArguments(it, icon, name, path) }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return HostApplication(
            id = id,
            name = name,
            exec = exec,
            workingDirectory = keys["Path"]?.let(DesktopEntryValues::unescape)?.takeIf { it.isNotBlank() },
            icon = icon,
            path = path,
            isSystem = isSystem,
            isGame = "Game" in keys.list("Categories")
        )
    }

    private fun Candidate.localized(key: String, locale: Locale): String? {
        val country = locale.country.takeIf { it.isNotEmpty() }
        val keysToTry = listOfNotNull(
            country?.let { "$key[${locale.language}_$it]" },
            "$key[${locale.language}]",
            key
        )
        return keysToTry.firstNotNullOfOrNull { keys[it]?.takeIf(String::isNotBlank) }
            ?.let(DesktopEntryValues::unescape)
    }

    private fun isSystemRoot(root: String, userDataHome: String): Boolean =
        !root.startsWith("$userDataHome/") && "/flatpak/exports/" !in root && "/snapd/" !in root

    private fun Map<String, String>.isTrue(key: String) = this[key].equals("true", ignoreCase = true)

    private fun Map<String, String>.list(key: String): List<String> =
        this[key]?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
}
