package com.nendo.argosy

import javax.inject.Inject

actual class ArgosyActivity @Inject constructor() {
    actual var isOverlayFocused: Boolean = false

    actual fun requestScreenCapturePermission() {
    }

    actual var isOnHomeScreen: Boolean = false
}
