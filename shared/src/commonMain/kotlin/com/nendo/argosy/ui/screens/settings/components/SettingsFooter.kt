package com.nendo.argosy.ui.screens.settings.components

import androidx.compose.runtime.Composable
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.ui.components.FooterHints
import com.nendo.argosy.ui.components.InputButton

@Composable
fun SettingsFooter() {
    FooterHints(
        hints = listOf(
            InputButton.DPAD to stringResource(R.string.settings_shell_footer_standalone_navigate),
            InputButton.A to stringResource(R.string.settings_shell_footer_standalone_select),
            InputButton.B to stringResource(R.string.settings_shell_footer_standalone_back)
        )
    )
}
