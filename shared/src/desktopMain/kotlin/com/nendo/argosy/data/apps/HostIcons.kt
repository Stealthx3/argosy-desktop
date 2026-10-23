package com.nendo.argosy.data.apps

import com.nendo.argosy.platform.HostShell

/**
 * Host icon files for a desktop entry's `Icon` key: an absolute path as is, a name looked up in
 * the hicolor theme first, then any other theme's `apps` icons, then `pixmaps`. PNG at 128 px or
 * larger wins over scalable SVG, which wins over smaller PNG.
 */
object HostIcons {
    private val INDEX_SCRIPT = """
        for d in "${'$'}@"; do
          [ -d "${'$'}d" ] || continue
          find -L "${'$'}d" \( -path '*/apps/*' -o -path '*/pixmaps/*' \) \( -name '*.png' -o -name '*.svg' \) -type f 2>/dev/null
        done
    """.trimIndent()

    private const val PIXMAPS = "/usr/share/pixmaps"

    @Volatile
    private var index: Map<String, String>? = null

    fun pathFor(icon: String): String? {
        if (icon.startsWith("/")) return icon
        val names = index ?: buildIndex().also { index = it }
        return names[icon] ?: names[icon.removeSuffix(".png").removeSuffix(".svg")]
    }

    private fun buildIndex(): Map<String, String> {
        val roots = HostApplications.dataDirectories().map { "$it/icons" } + PIXMAPS
        val files = HostShell.runScript(INDEX_SCRIPT, roots)?.lines()?.filter { it.isNotBlank() } ?: return emptyMap()
        return bestByName(files)
    }

    internal fun bestByName(files: List<String>): Map<String, String> = files
        .groupBy { it.substringAfterLast('/').substringBeforeLast('.') }
        .mapValues { (_, paths) -> paths.maxBy(::score) }

    private fun score(path: String): Int {
        val theme = when {
            "/icons/hicolor/" in path -> 3_000
            "/icons/" in path -> 2_000
            else -> 1_000
        }
        val pixels = Regex("/(\\d+)x\\d+(@\\d+x)?/").find(path)?.groupValues?.get(1)?.toIntOrNull()
        val size = when {
            path.endsWith(".svg") -> 500
            pixels == null -> 100
            pixels >= 128 -> 600 + pixels.coerceAtMost(256)
            else -> pixels
        }
        return theme + size
    }
}
