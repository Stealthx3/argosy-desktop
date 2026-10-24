package com.nendo.argosy.ui.input

import javax.inject.Inject

actual class NavigationButtonCapture @Inject constructor() {
    actual val supported: Boolean = false

    actual fun activeController(): MappedController? = null

    actual fun captureNext(listener: (controller: String, keyCode: Int) -> Unit) = Unit

    actual fun cancel() = Unit
}
