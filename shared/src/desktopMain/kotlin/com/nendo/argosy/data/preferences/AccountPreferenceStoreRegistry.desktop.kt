package com.nendo.argosy.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class AccountPreferenceStoreRegistry actual constructor(
    private val context: Context,
    globalStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val stores = mutableMapOf<Long, DataStore<Preferences>>()

    actual companion object {}

    actual suspend fun storeFor(rommUserId: Long): DataStore<Preferences> = mutex.withLock {
        stores.getOrPut(rommUserId) {
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
            ) {
                context.preferencesDataStoreFile("account_$rommUserId")
            }
        }
    }

    actual suspend fun clearFor(rommUserId: Long) {
        storeFor(rommUserId).edit { it.clear() }
    }
}
