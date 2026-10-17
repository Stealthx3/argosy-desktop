package com.nendo.argosy.data.netplay

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.MODE_PRIVATE
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformSharedPreferences as SharedPreferences
import com.nendo.argosy.config.AppBuildConfig as BuildConfig
import com.nendo.argosy.di.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoreHashCache @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        MODE_PRIVATE
    )

    private val memoryCache = ConcurrentHashMap<String, String>()

    init {
        val cachedVersion = prefs.getInt(KEY_VERSION_CODE, -1)
        if (cachedVersion != BuildConfig.VERSION_CODE) {
            prefs.edit().clear().putInt(KEY_VERSION_CODE, BuildConfig.VERSION_CODE).apply()
        }
    }

    fun getHashForCore(corePath: String): String? {
        memoryCache[corePath]?.let { return it }

        val persistedKey = keyFor(corePath)
        val persisted = prefs.getString(persistedKey, null)
        if (persisted != null) {
            memoryCache[corePath] = persisted
            return persisted
        }

        val file = File(corePath)
        if (!file.exists() || !file.isFile) return null

        val computed = try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            return null
        }

        memoryCache[corePath] = computed
        prefs.edit().putString(persistedKey, computed).apply()
        return computed
    }

    fun invalidate(corePath: String) {
        memoryCache.remove(corePath)
        prefs.edit().remove(keyFor(corePath)).apply()
    }

    private fun keyFor(corePath: String): String = "core_hash:$corePath"

    companion object {
        private const val PREFS_NAME = "argosy_netplay_core_hashes"
        private const val KEY_VERSION_CODE = "version_code"
    }
}
