package com.nendo.argosy.ui.screens.common

import com.nendo.argosy.platform.PlatformContext as Context
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
import com.nendo.argosy.platform.PlatformBundle as Bundle
import com.nendo.argosy.DualScreenManagerHolder
import com.nendo.argosy.data.preferences.EmulatorDisplayTarget
import com.nendo.argosy.data.preferences.SessionStateStore
import com.nendo.argosy.data.preferences.UserPreferencesRepository
import com.nendo.argosy.data.repository.EmulatorConfigRepository
import com.nendo.argosy.util.DisplayAffinityHelper
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmulatorLaunchTargetResolver @Inject constructor(
    @ApplicationContext context: Context,
    private val displayAffinityHelper: DisplayAffinityHelper,
    private val emulatorConfigRepository: EmulatorConfigRepository,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val sessionStateStore by lazy { SessionStateStore(context) }

    suspend fun launchOptionsFor(gameId: Long, overrideDisplayId: Int? = null): Bundle? {
        val rolesSwapped = sessionStateStore.isRolesSwapped()
        val displayId = overrideDisplayId ?: targetDisplayId(
            stored = EmulatorDisplayTarget.fromString(
                emulatorConfigRepository.getEffectiveDisplayTarget(gameId)
            ),
            gamesDefault = preferencesRepository.userPreferences.first().gamesDisplayTarget,
            rolesSwapped = rolesSwapped
        )
        DualScreenManagerHolder.instance?.setEmulatorDisplay(placedOn(displayId, rolesSwapped))
        return displayAffinityHelper.getActivityOptions(
            forEmulator = true,
            rolesSwapped = rolesSwapped,
            overrideDisplayId = displayId
        )
    }

    /**
     * The display a game opens on when [stored] is its effective target, the way [launchOptionsFor]
     * places it.
     */
    fun placementDisplayId(
        stored: EmulatorDisplayTarget,
        gamesDefault: EmulatorDisplayTarget,
        rolesSwapped: Boolean
    ): Int = placedOn(targetDisplayId(stored, gamesDefault, rolesSwapped), rolesSwapped)

    private fun targetDisplayId(
        stored: EmulatorDisplayTarget,
        gamesDefault: EmulatorDisplayTarget,
        rolesSwapped: Boolean
    ): Int? = displayAffinityHelper.getDisplayTargetId(placementTarget(stored, gamesDefault), rolesSwapped)

    private fun placedOn(displayId: Int?, rolesSwapped: Boolean): Int =
        displayId ?: displayAffinityHelper.getEmulatorDisplayId(rolesSwapped)

    companion object {
        fun placementTarget(
            stored: EmulatorDisplayTarget,
            gamesDefault: EmulatorDisplayTarget
        ): EmulatorDisplayTarget = when {
            stored != EmulatorDisplayTarget.DEFAULT -> stored
            gamesDefault == EmulatorDisplayTarget.PRESENTATION -> EmulatorDisplayTarget.DEFAULT
            else -> gamesDefault
        }

        fun resolvedTarget(
            stored: EmulatorDisplayTarget,
            gamesDefault: EmulatorDisplayTarget
        ): EmulatorDisplayTarget =
            if (stored != EmulatorDisplayTarget.DEFAULT) stored
            else EmulatorDisplayTarget.gamesDefaultFrom(gamesDefault.name)
    }
}
