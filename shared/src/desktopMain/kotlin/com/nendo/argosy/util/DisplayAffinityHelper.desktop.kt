package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformBundle as Bundle
import com.nendo.argosy.data.preferences.EmulatorDisplayTarget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class DisplayAffinityHelper @Inject constructor() {
    actual companion object {
        actual fun isKnownDualScreenDevice(): Boolean = false

        actual fun hasInvertedInternalOrder(): Boolean = false

        actual fun dockedExternalDisplayId(context: Context): Int? = null

        actual const val BLANK_PANELS_ON_VIDEO_OUTPUT_SETTING: String = "close_screen_when_output_video"
    }

    actual val hasPhysicalSecondaryDisplay: Boolean = false

    actual var dualScreenEnabled: Boolean = false

    actual var secondaryDisplayUsable: Boolean = false

    actual var appTargetDisplayId: Int? = null

    actual var offDisplayIds: Set<Int> = emptySet()

    actual var desktopModeDisplayId: Int? = null

    actual val isDesktopMode: Boolean = false

    actual val dockedDisplayId: Int? = null

    actual val isDockedDark: Boolean = false

    actual val hasSecondaryDisplay: Boolean = false

    actual val secondaryDisplayType: SecondaryDisplayType = TODO("desktop: secondaryDisplayType")

    actual var roleDisplayIds: Pair<Int, Int>? = null

    actual fun largestDisplayId(): Int? = null

    actual fun getCompanionLaunchOptions(): Bundle? = null

    actual fun appScreenDisplayId(rolesSwapped: Boolean): Int? = null

    actual fun getAppScreenLaunchOptions(rolesSwapped: Boolean): Bundle? = null

    actual fun getEmulatorDisplayId(rolesSwapped: Boolean): Int = 0

    actual fun appLaunchDisplayId(preferredScreenKey: String?, rolesSwapped: Boolean, occupiedDisplayId: Int?, defaultTarget: EmulatorDisplayTarget): Int? = null

    actual fun getAppLaunchOptions(preferredScreenKey: String?, rolesSwapped: Boolean, occupiedDisplayId: Int?, defaultTarget: EmulatorDisplayTarget): Bundle? = null

    actual fun getDisplayTargetId(target: EmulatorDisplayTarget, rolesSwapped: Boolean): Int? = null

    actual fun getRoleDisplayIds(rolesSwapped: Boolean): Pair<Int, Int>? = null

    actual fun getMediaPlayerDisplayId(emulatorDisplayId: Int?): Int? = null

    actual fun displayContext(displayId: Int): Context? = null

    actual fun screenNumbersByDisplayId(): Map<Int, Int> = emptyMap()

    actual fun isBuiltInDisplay(displayId: Int): Boolean = false

    actual fun getActivityOptions(forEmulator: Boolean, rolesSwapped: Boolean, overrideDisplayId: Int?): Bundle? = null

    actual fun isPhysicalDisplay(displayId: Int): Boolean = false
}
