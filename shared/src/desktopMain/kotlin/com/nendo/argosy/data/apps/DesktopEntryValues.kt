package com.nendo.argosy.data.apps

/**
 * Value decoding from the Desktop Entry Specification: string escapes, and the `Exec` key's
 * quoting and field codes. File and URL field codes expand to the given files (`%f`/`%u` to the
 * first, `%F`/`%U` to all; URL codes as `file://` URIs) or to nothing; `%i`, `%c` and `%k` expand
 * to the icon, the name and the entry's own path.
 */
object DesktopEntryValues {
    fun unescape(value: String): String {
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    's' -> out.append(' ')
                    'n' -> out.append('\n')
                    't' -> out.append('\t')
                    'r' -> out.append('\r')
                    '\\' -> out.append('\\')
                    else -> out.append('\\').append(next)
                }
                index += 2
            } else {
                out.append(char)
                index++
            }
        }
        return out.toString()
    }

    fun execArguments(
        exec: String,
        icon: String?,
        name: String,
        entryPath: String,
        files: List<String> = emptyList()
    ): List<String> =
        splitQuoted(unescape(exec)).flatMap { expandFieldCodes(it, icon, name, entryPath, files) }

    fun takesFiles(exec: String): Boolean =
        splitQuoted(unescape(exec)).any { !it.quoted && it.text in FILE_CODES }

    private fun splitQuoted(command: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val current = StringBuilder()
        var quoted = false
        var inQuotes = false
        var started = false
        var index = 0
        while (index < command.length) {
            val char = command[index]
            when {
                inQuotes && char == '\\' && index + 1 < command.length && command[index + 1] in QUOTE_ESCAPES -> {
                    current.append(command[index + 1])
                    index++
                }
                char == '"' -> {
                    inQuotes = !inQuotes
                    quoted = true
                    started = true
                }
                !inQuotes && char.isWhitespace() -> {
                    if (started) tokens += Token(current.toString(), quoted)
                    current.clear()
                    quoted = false
                    started = false
                }
                else -> {
                    current.append(char)
                    started = true
                }
            }
            index++
        }
        if (started) tokens += Token(current.toString(), quoted)
        return tokens
    }

    private fun expandFieldCodes(
        token: Token,
        icon: String?,
        name: String,
        entryPath: String,
        files: List<String>
    ): List<String> {
        if (!token.quoted) {
            when (token.text) {
                "%f" -> return files.take(1)
                "%F" -> return files
                "%u" -> return files.take(1).map(::fileUri)
                "%U" -> return files.map(::fileUri)
                "%d", "%D", "%n", "%N", "%v", "%m" -> return emptyList()
                "%i" -> return icon?.let { listOf("--icon", it) }.orEmpty()
            }
        }
        val out = StringBuilder()
        var index = 0
        while (index < token.text.length) {
            val char = token.text[index]
            if (char == '%' && index + 1 < token.text.length) {
                when (token.text[index + 1]) {
                    '%' -> out.append('%')
                    'c' -> out.append(name)
                    'k' -> out.append(entryPath)
                    'i' -> icon?.let { out.append(it) }
                }
                index += 2
            } else {
                out.append(char)
                index++
            }
        }
        return listOf(out.toString())
    }

    private fun fileUri(path: String): String = java.nio.file.Path.of(path).toUri().toASCIIString()

    private data class Token(val text: String, val quoted: Boolean)

    private val QUOTE_ESCAPES = setOf('"', '`', '$', '\\')

    private val FILE_CODES = setOf("%f", "%F", "%u", "%U")
}
