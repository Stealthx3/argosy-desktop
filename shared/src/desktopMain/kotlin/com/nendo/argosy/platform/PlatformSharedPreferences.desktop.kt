package com.nendo.argosy.platform

import java.io.File
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

actual interface PlatformSharedPreferences {
    actual fun getString(key: String?, defValue: String?): String?
    actual fun getInt(key: String?, defValue: Int): Int
    actual fun getLong(key: String?, defValue: Long): Long
    actual fun getBoolean(key: String?, defValue: Boolean): Boolean
    actual fun contains(key: String?): Boolean
    actual fun edit(): Editor

    actual interface Editor {
        actual fun putString(key: String?, value: String?): Editor
        actual fun putInt(key: String?, value: Int): Editor
        actual fun putLong(key: String?, value: Long): Editor
        actual fun putBoolean(key: String?, value: Boolean): Editor
        actual fun remove(key: String?): Editor
        actual fun clear(): Editor
        actual fun commit(): Boolean
        actual fun apply()
    }
}

private val openPreferences = ConcurrentHashMap<String, FilePreferences>()

actual fun PlatformContext.getSharedPreferences(name: String?, mode: Int): PlatformSharedPreferences =
    openPreferences.getOrPut(name.orEmpty()) {
        FilePreferences(filesDir.resolve("shared_prefs").resolve("${name.orEmpty()}.properties"))
    }

private class FilePreferences(private val file: File) : PlatformSharedPreferences {
    private val stored = ConcurrentHashMap<String, String>()

    init {
        if (file.exists()) {
            Properties().apply { file.inputStream().use(::load) }.forEach { (k, v) -> stored[k.toString()] = v.toString() }
        }
    }

    override fun getString(key: String?, defValue: String?): String? = stored[key.orEmpty()]?.removePrefix("s:") ?: defValue
    override fun getInt(key: String?, defValue: Int): Int = stored[key.orEmpty()]?.removePrefix("i:")?.toIntOrNull() ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = stored[key.orEmpty()]?.removePrefix("l:")?.toLongOrNull() ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        stored[key.orEmpty()]?.removePrefix("b:")?.toBooleanStrictOrNull() ?: defValue
    override fun contains(key: String?): Boolean = stored.containsKey(key.orEmpty())
    override fun edit(): PlatformSharedPreferences.Editor = FileEditor()

    @Synchronized
    private fun write(changes: Map<String, String?>, clear: Boolean) {
        if (clear) stored.clear()
        changes.forEach { (k, v) -> if (v == null) stored.remove(k) else stored[k] = v }
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        temp.outputStream().use { out -> Properties().apply { putAll(stored) }.store(out, null) }
        temp.renameTo(file)
    }

    private inner class FileEditor : PlatformSharedPreferences.Editor {
        private val changes = LinkedHashMap<String, String?>()
        private var clear = false

        override fun putString(key: String?, value: String?) = apply { changes[key.orEmpty()] = value?.let { "s:$it" } }
        override fun putInt(key: String?, value: Int) = apply { changes[key.orEmpty()] = "i:$value" }
        override fun putLong(key: String?, value: Long) = apply { changes[key.orEmpty()] = "l:$value" }
        override fun putBoolean(key: String?, value: Boolean) = apply { changes[key.orEmpty()] = "b:$value" }
        override fun remove(key: String?) = apply { changes[key.orEmpty()] = null }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean {
            write(changes, clear)
            return true
        }
        override fun apply() {
            write(changes, clear)
        }
    }
}
