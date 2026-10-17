package com.nendo.argosy.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class ScreenCatalog @Inject constructor() {
    actual companion object {}

    actual fun attachedScreens(): List<AttachedScreen> = emptyList()

    actual fun screenFor(displayId: Int): AttachedScreen? = null
}
