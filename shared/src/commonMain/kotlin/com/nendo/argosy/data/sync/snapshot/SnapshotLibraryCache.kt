package com.nendo.argosy.data.sync.snapshot

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.util.Logger
import com.squareup.moshi.Moshi
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last snapshot library fetched for each account and game, kept in memory and on disk so the
 * save manager can draw it before the server answers.
 */
@Singleton
class SnapshotLibraryCache @Inject constructor(
    @ApplicationContext context: Context,
    moshi: Moshi
) {
    private val adapter = moshi.adapter(SnapshotLibrary::class.java)
    private val dir = File(context.filesDir, DIR)
    private val memory = ConcurrentHashMap<String, SnapshotLibrary>()

    suspend fun get(ownerUserId: Long, gameId: Long): SnapshotLibrary? = withContext(Dispatchers.IO) {
        val key = key(ownerUserId, gameId)
        memory[key] ?: runCatching { File(dir, "$key.json").takeIf { it.isFile }?.readText()?.let(adapter::fromJson) }
            .onFailure { Logger.warn(TAG, "unreadable cached library $key: ${it.message}") }
            .getOrNull()
            ?.also { memory[key] = it }
    }

    suspend fun put(ownerUserId: Long, gameId: Long, library: SnapshotLibrary) = withContext(Dispatchers.IO) {
        val key = key(ownerUserId, gameId)
        memory[key] = library
        runCatching {
            dir.mkdirs()
            val target = File(dir, "$key.json")
            val temp = File(dir, "$key.json.tmp")
            temp.writeText(adapter.toJson(library))
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        }.onFailure { Logger.warn(TAG, "could not store library $key: ${it.message}") }
    }

    private fun key(ownerUserId: Long, gameId: Long) = "${ownerUserId}_$gameId"

    private companion object {
        const val TAG = "SnapshotLibraryCache"
        const val DIR = "snapshot_library"
    }
}
