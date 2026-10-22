package com.nendo.argosy

expect class ArgosyActivity {
    var isOnHomeScreen: Boolean

    var isOverlayFocused: Boolean

    fun requestScreenCapturePermission()
}
