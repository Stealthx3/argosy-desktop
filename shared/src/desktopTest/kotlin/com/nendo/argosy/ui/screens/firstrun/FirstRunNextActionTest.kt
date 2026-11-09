package com.nendo.argosy.ui.screens.firstrun

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirstRunNextActionTest {
    private fun at(step: FirstRunStep) = FirstRunUiState(currentStep = step)

    @Test
    fun everyStepWithAContinueButtonHasANextAction() {
        assertEquals(FirstRunNext.ADVANCE, firstRunNextAction(at(FirstRunStep.WELCOME)))
        assertEquals(FirstRunNext.CHOOSE_DISPLAY, firstRunNextAction(at(FirstRunStep.DISPLAY_SETUP)))
        assertEquals(FirstRunNext.ADVANCE, firstRunNextAction(at(FirstRunStep.ROMM_SUCCESS)))
        assertEquals(FirstRunNext.PLATFORMS, firstRunNextAction(at(FirstRunStep.PLATFORM_SELECT)))
        assertEquals(FirstRunNext.ADVANCE, firstRunNextAction(at(FirstRunStep.CORE_PROMPT)))
        assertEquals(FirstRunNext.COMPLETE, firstRunNextAction(at(FirstRunStep.COMPLETE)))
    }

    @Test
    fun rommLoginContinuesOnlyWithSomethingToSubmit() {
        val login = at(FirstRunStep.ROMM_LOGIN)
        assertNull(firstRunNextAction(login))
        assertEquals(FirstRunNext.COMMIT_URL, firstRunNextAction(login.copy(rommUrl = "http://romm")))
        assertNull(firstRunNextAction(login.copy(rommUrl = "http://romm", isConnecting = true)))
        assertEquals(
            FirstRunNext.CONNECT,
            firstRunNextAction(login.copy(rommUrl = "http://romm", rommUrlCommitted = true))
        )
        assertNull(firstRunNextAction(login.copy(rommUrl = "http://romm", rommDevicePairing = true)))
    }

    @Test
    fun foldersAndDownloadsGateTheNextAction() {
        assertNull(firstRunNextAction(at(FirstRunStep.ROM_PATH)))
        assertEquals(FirstRunNext.ROM_PATH, firstRunNextAction(at(FirstRunStep.ROM_PATH).copy(folderSelected = true)))
        assertEquals(FirstRunNext.DEFAULT_IMAGE_CACHE, firstRunNextAction(at(FirstRunStep.IMAGE_CACHE)))
        assertEquals(
            FirstRunNext.IMAGE_CACHE,
            firstRunNextAction(at(FirstRunStep.IMAGE_CACHE).copy(imageCacheFolderSelected = true))
        )
        assertNull(firstRunNextAction(at(FirstRunStep.CORE_DOWNLOAD)))
        assertEquals(
            FirstRunNext.ADVANCE,
            firstRunNextAction(at(FirstRunStep.CORE_DOWNLOAD).copy(coreDownloadComplete = true))
        )
        assertNull(firstRunNextAction(at(FirstRunStep.PLATFORM_SELECT).copy(platformSearchActive = true)))
    }

    @Test
    fun permissionsNeedStorageAccess() {
        assertNull(firstRunNextAction(at(FirstRunStep.PERMISSIONS)))
        assertEquals(
            FirstRunNext.PERMISSIONS,
            firstRunNextAction(at(FirstRunStep.PERMISSIONS).copy(hasStoragePermission = true))
        )
    }

    @Test
    fun safeAreaRowOnlyWithTvSelected() {
        val tv = com.nendo.argosy.data.preferences.DisplayProfile.TV
        val tvIndex = displaySetupChoices().indexOf(tv)
        val onTv = at(FirstRunStep.DISPLAY_SETUP).copy(displaySelectedIndex = tvIndex)
        assertEquals(
            listOf(DisplaySetupRow.FULL_SCREEN, DisplaySetupRow.SAFE_AREA, DisplaySetupRow.UI_SCALE),
            displaySetupRows(onTv)
        )
        assertEquals(tv.defaultSafeAreaPercent, displaySetupSafeArea(onTv))
        assertEquals(0, displaySetupSafeArea(onTv.copy(safeAreaOverride = 0)))
        val desktopIndex = displaySetupChoices().indexOf(com.nendo.argosy.data.preferences.DisplayProfile.DESKTOP)
        if (desktopIndex >= 0) {
            val onDesktop = onTv.copy(displaySelectedIndex = desktopIndex, displayRowFocus = 1)
            assertEquals(listOf(DisplaySetupRow.FULL_SCREEN, DisplaySetupRow.UI_SCALE), displaySetupRows(onDesktop))
            assertEquals(DisplaySetupRow.UI_SCALE, focusedDisplayRow(onDesktop))
        }
    }
}
