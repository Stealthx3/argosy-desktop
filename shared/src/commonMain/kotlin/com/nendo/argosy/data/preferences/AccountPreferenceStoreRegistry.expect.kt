package com.nendo.argosy.data.preferences

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope

expect class AccountPreferenceStoreRegistry(context: Context, globalStore: DataStore<Preferences>, scope: CoroutineScope) {
    companion object {}

    suspend fun storeFor(rommUserId: Long): DataStore<Preferences>

    suspend fun clearFor(rommUserId: Long)
}
