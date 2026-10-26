package com.nendo.argosy.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickSettingsConnectionsTest {

    private val radios = listOf(QuickSettingsItem.Wifi, QuickSettingsItem.Bluetooth)

    @Test
    fun `radio rows show on every device`() {
        listOf(false, true).forEach { switchable ->
            val visible = quickSettingsVisibleItems(QuickSettingsPage.QUICK, QuickSettingsState(radiosSwitchable = switchable))
            assertTrue(visible.containsAll(radios))
            assertTrue(visible.any { it is QuickSettingsItem.Header && it.headerGroup == QuickSettingsGroup.CONNECTIONS })
        }
    }

    @Test
    fun `radio rows take focus only where the radios can be switched`() {
        val limited = quickSettingsFocusableItems(QuickSettingsPage.QUICK, QuickSettingsState(radiosSwitchable = false))
        val switchable = quickSettingsFocusableItems(QuickSettingsPage.QUICK, QuickSettingsState(radiosSwitchable = true))

        radios.forEach { assertFalse(it in limited) }
        assertTrue(switchable.containsAll(radios))
    }
}
