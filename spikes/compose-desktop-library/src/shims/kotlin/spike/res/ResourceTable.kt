package spike.res

/** Upstream's English strings, plurals and drawable names, keyed by the ids in the generated R. */
object ResourceTable {
    private class Entry(val kind: String, val name: String, val value: String)

    private val entries: Map<Int, Entry> by lazy {
        val text = ResourceTable::class.java.getResourceAsStream("/spike/resources.tsv")!!
            .bufferedReader().readText()
        text.lineSequence().filter { it.isNotEmpty() }.associate { line ->
            val (id, kind, name, value) = line.split('\t', limit = 4)
            id.toInt() to Entry(kind, name, value)
        }
    }

    private fun unescape(s: String) = s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\")

    fun string(id: Int): String = unescape(entries.getValue(id).value)

    fun string(id: Int, vararg args: Any?): String = string(id).format(*args)

    /** English plural rules: "one" for 1, otherwise "other". */
    fun quantity(id: Int, count: Int, vararg args: Any?): String {
        val forms = entries.getValue(id).value.split('\u001f').associate {
            val (q, v) = it.split('=', limit = 2); q to unescape(v)
        }
        val form = (if (count == 1) forms["one"] else null) ?: forms["other"] ?: forms.values.first()
        return form.format(*args)
    }

    fun drawableName(id: Int): String = entries.getValue(id).name
}
