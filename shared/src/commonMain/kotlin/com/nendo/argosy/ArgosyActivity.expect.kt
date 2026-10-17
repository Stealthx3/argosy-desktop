package com.nendo.argosy

expect class ArgosyActivity {
    var isOverlayFocused: Boolean

    fun requestScreenCapturePermission()
}
