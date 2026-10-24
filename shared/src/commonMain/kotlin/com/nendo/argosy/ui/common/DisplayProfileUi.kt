package com.nendo.argosy.ui.common

import com.nendo.argosy.data.preferences.DisplayProfile
import com.nendo.argosy.res.ArgosyR as R

val DisplayProfile.labelRes: Int
    get() = when (this) {
        DisplayProfile.HANDHELD -> R.string.settings_interface_display_profile_handheld
        DisplayProfile.DESKTOP -> R.string.settings_interface_display_profile_desktop
        DisplayProfile.TV -> R.string.settings_interface_display_profile_tv
    }

val DisplayProfile.detailRes: Int
    get() = when (this) {
        DisplayProfile.HANDHELD -> R.string.settings_interface_display_profile_handheld_detail
        DisplayProfile.DESKTOP -> R.string.settings_interface_display_profile_desktop_detail
        DisplayProfile.TV -> R.string.settings_interface_display_profile_tv_detail
    }
