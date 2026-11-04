package com.nendo.argosy.ui.screens.settings.sections

import com.nendo.argosy.ui.screens.settings.ControlsState
import kotlin.test.Test
import kotlin.test.assertEquals

class MainSettingsCloseRowTest {
    @Test
    fun closeRowIsTheLastRowOfTheSettingsPage() {
        val controls = ControlsState()
        val last = mainSettingsItemAtFocusIndex(mainSettingsMaxFocusIndex(controls), controls)
        assertEquals(MainSettingsItem.CloseApp, last)
    }
}
