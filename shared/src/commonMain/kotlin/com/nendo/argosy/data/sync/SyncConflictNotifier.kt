package com.nendo.argosy.data.sync

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.core.notification.NotificationDuration
import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.NotificationType
import com.nendo.argosy.data.local.dao.PendingConflictDao
import com.nendo.argosy.data.local.entity.PendingConflictEntity
import com.nendo.argosy.data.preferences.SyncPreferencesRepository
import com.nendo.argosy.util.Logger
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncConflictNotifier"
private const val NOTIFICATION_KEY = "sync_conflict_pending"

@Singleton
class SyncConflictNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingConflictDao: PendingConflictDao,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val notificationManager: NotificationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var lastCount: Int = 0

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun start() {
        scope.launch {
            syncPreferencesRepository.preferences
                .map { it.rommUserId }
                .distinctUntilChanged()
                .flatMapLatest { pendingConflictDao.getOpenCountFlow(PendingConflictEntity.ownerScope(it)) }
                .distinctUntilChanged()
                .collect { count -> onCountChanged(count) }
        }
    }

    private fun onCountChanged(count: Int) {
        val previous = lastCount
        lastCount = count
        when {
            count <= 0 -> {
                if (previous > 0) {
                    notificationManager.dismissByKey(NOTIFICATION_KEY)
                    Logger.debug(TAG, "Conflicts cleared; notification dismissed")
                }
            }
            count > previous -> {
                Logger.info(TAG, "Conflict count rose $previous -> $count; surfacing notification")
                notificationManager.show(
                    title = NotificationText.Plural(
                        R.plurals.sync_conflict_pending_title,
                        count,
                        listOf(count)
                    ),
                    subtitle = NotificationText.Res(R.string.sync_conflict_pending_subtitle),
                    type = NotificationType.WARNING,
                    duration = NotificationDuration.LONG,
                    key = NOTIFICATION_KEY,
                    immediate = false
                )
            }
        }
    }
}
