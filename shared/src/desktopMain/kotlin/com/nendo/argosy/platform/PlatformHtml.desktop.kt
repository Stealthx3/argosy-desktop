package com.nendo.argosy.platform

actual object PlatformHtml {
    actual val FROM_HTML_MODE_LEGACY: Int = 0

    private val entities = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ")

    actual fun fromHtml(source: String, flags: Int): CharSequence {
        val text = source
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>\\s*"), "\n\n")
            .replace(Regex("<[^>]*>"), "")
        return Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);").replace(text) { match ->
            val name = match.groupValues[1]
            when {
                name.startsWith("#x") || name.startsWith("#X") -> name.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
                name.startsWith("#") -> name.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }
                else -> entities[name]
            } ?: match.value
        }.trimEnd('\n')
    }
}
