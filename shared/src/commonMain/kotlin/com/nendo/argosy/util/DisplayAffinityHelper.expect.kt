package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
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
import com.nendo.argosy.data.preferences.EmulatorDisplayTarget

expect class DisplayAffinityHelper {
    companion object {
        fun isKnownDualScreenDevice(): Boolean

        fun hasInvertedInternalOrder(): Boolean

        fun dockedExternalDisplayId(context: Context): Int?

        val BLANK_PANELS_ON_VIDEO_OUTPUT_SETTING: String

        internal fun rolesSwappedForPrimary(primaryDisplayId: Int): Boolean
    }

    val hasPhysicalSecondaryDisplay: Boolean

    var dualScreenEnabled: Boolean

    var secondaryDisplayUsable: Boolean

    var appTargetDisplayId: Int?

    var offDisplayIds: Set<Int>

    var desktopModeDisplayId: Int?

    val isDesktopMode: Boolean

    val dockedDisplayId: Int?

    val isDockedDark: Boolean

    val hasSecondaryDisplay: Boolean

    val secondaryDisplayType: SecondaryDisplayType

    var roleDisplayIds: Pair<Int, Int>?

    fun largestDisplayId(): Int?

    fun getCompanionLaunchOptions(): Bundle?

    fun appScreenDisplayId(rolesSwapped: Boolean): Int?

    fun getAppScreenLaunchOptions(rolesSwapped: Boolean): Bundle?

    fun getEmulatorDisplayId(rolesSwapped: Boolean): Int

    fun appLaunchDisplayId(preferredScreenKey: String? = null, rolesSwapped: Boolean = false, occupiedDisplayId: Int? = null, defaultTarget: EmulatorDisplayTarget = EmulatorDisplayTarget.DEFAULT): Int?

    fun getAppLaunchOptions(preferredScreenKey: String? = null, rolesSwapped: Boolean = false, occupiedDisplayId: Int? = null, defaultTarget: EmulatorDisplayTarget = EmulatorDisplayTarget.DEFAULT): Bundle?

    fun getDisplayTargetId(target: EmulatorDisplayTarget, rolesSwapped: Boolean): Int?

    fun getRoleDisplayIds(rolesSwapped: Boolean): Pair<Int, Int>?

    fun getMediaPlayerDisplayId(emulatorDisplayId: Int?): Int?

    fun displayContext(displayId: Int): Context?

    fun screenNumbersByDisplayId(): Map<Int, Int>

    fun isBuiltInDisplay(displayId: Int): Boolean

    fun getActivityOptions(forEmulator: Boolean, rolesSwapped: Boolean = false, overrideDisplayId: Int? = null): Bundle?

    fun isPhysicalDisplay(displayId: Int): Boolean
}
