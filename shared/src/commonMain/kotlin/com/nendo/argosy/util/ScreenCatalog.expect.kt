package com.nendo.argosy.util

expect class ScreenCatalog {
    companion object {}

    fun attachedScreens(): List<AttachedScreen>

    fun screenFor(displayId: Int): AttachedScreen?
}
