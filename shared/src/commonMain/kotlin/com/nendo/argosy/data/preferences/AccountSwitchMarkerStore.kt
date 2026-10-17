package com.nendo.argosy.data.preferences

import com.nendo.argosy.platform.PlatformContext as Context
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
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one durable answer to "is a RomM account switch under way right now".
 *
 * Backed by SharedPreferences rather than DataStore so a launch attempt or a worker can read it
 * without suspending, and so it is already readable the moment the process comes up. Every
 * writer of live save bytes consults this before touching disk; a switch tears down one account's
 * saves and places another's, and a background write landing in that window writes the wrong
 * account's progress into the wrong account's slot.
 */
@Singleton
class AccountSwitchMarkerStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val store = SessionStateStore(context)

    private val _marker = MutableStateFlow(store.getAccountSwitchMarker())
    val marker: StateFlow<SessionStateStore.AccountSwitchMarker?> = _marker.asStateFlow()

    fun isSwitching(): Boolean = store.isAccountSwitchInProgress()

    fun current(): SessionStateStore.AccountSwitchMarker? = store.getAccountSwitchMarker()

    /**
     * Claims the switch slot. Returns false when a switch is already marked, which is how a
     * second concurrent switch is refused.
     */
    @Synchronized
    fun begin(fromUserId: Long?, toUserId: Long): Boolean {
        if (store.isAccountSwitchInProgress()) return false
        store.setAccountSwitchMarker(fromUserId, toUserId, System.currentTimeMillis())
        _marker.value = store.getAccountSwitchMarker()
        return true
    }

    @Synchronized
    fun clear() {
        store.clearAccountSwitchMarker()
        _marker.value = null
    }

    @Synchronized
    fun refresh() {
        _marker.value = store.getAccountSwitchMarker()
    }
}
