package com.nendo.argosy.platform

import com.nendo.argosy.ui.navigation.NavRing
import com.nendo.argosy.ui.navigation.Screen
import com.nendo.argosy.ui.screens.firstrun.FirstRunStep
import com.nendo.argosy.ui.screens.firstrun.stepAfterPlatformSelect
import com.nendo.argosy.ui.screens.firstrun.stepBeforeComplete
import com.nendo.argosy.ui.screens.settings.ControlsState
import com.nendo.argosy.ui.screens.settings.sections.MainSettingsItem
import com.nendo.argosy.ui.screens.settings.sections.mainSettingsItemAtFocusIndex
import com.nendo.argosy.ui.screens.settings.sections.mainSettingsMaxFocusIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformFeaturesTest {
    private val removedTokens = listOf(Screen.Social.route, Screen.QuayPass.route, Screen.MediaLibrary.route).map(NavRing::token)

    @Test
    fun desktopOffersNoneOfTheRemovedFeatures() {
        assertFalse(PlatformFeatures.steam)
        assertFalse(PlatformFeatures.jellyfin)
        assertFalse(PlatformFeatures.social)
        assertFalse(PlatformFeatures.deviceSettings)
        assertFalse(PlatformFeatures.builtInEmulation)
    }

    @Test
    fun mainSettingsHasNoRowForARemovedFeature() {
        val controls = ControlsState()
        val items = (0..mainSettingsMaxFocusIndex(controls)).mapNotNull { mainSettingsItemAtFocusIndex(it, controls) }
        val removed = listOf(
            MainSettingsItem.BuiltinEmulator, MainSettingsItem.Steam, MainSettingsItem.Jellyfin,
            MainSettingsItem.Social, MainSettingsItem.DeviceSettings
        )
        assertTrue(items.isNotEmpty())
        assertTrue(items.none { it in removed }, "visible: $items")
    }

    @Test
    fun drawerAndRingHaveNoSocialCheckInOrMediaPage() {
        assertTrue(NavRing.PAGES.none { NavRing.token(it.route) in removedTokens })
        assertTrue(NavRing.resolve(null).none { it in removedTokens })
        assertTrue(NavRing.resolve(removedTokens).none { it in removedTokens })
        assertTrue(NavRing.rows(NavRing.resolve(null)).none { it in removedTokens })
    }

    @Test
    fun firstRunSkipsTheCoreStepsWithoutBuiltInEmulation() {
        assertEquals(FirstRunStep.COMPLETE, stepAfterPlatformSelect())
        assertEquals(FirstRunStep.IMAGE_CACHE, stepBeforeComplete(rommPlatformCount = 4))
        assertEquals(FirstRunStep.PLATFORM_SELECT, stepBeforeComplete(rommPlatformCount = 12))
        assertEquals(FirstRunStep.CORE_PROMPT, stepAfterPlatformSelect(builtInEmulation = true))
        assertEquals(FirstRunStep.CORE_DOWNLOAD, stepBeforeComplete(rommPlatformCount = 4, builtInEmulation = true))
    }
}
