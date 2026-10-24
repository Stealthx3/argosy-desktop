package com.nendo.argosy.ui.input

/**
 * Hands the next physical press on a controller or the keyboard to the button-mapping rows, and
 * names the controller those rows edit. [supported] is false where navigation buttons cannot be
 * remapped.
 */
expect class NavigationButtonCapture {
    val supported: Boolean

    fun activeController(): MappedController?

    fun captureNext(listener: (controller: String, keyCode: Int) -> Unit)

    fun cancel()
}

data class MappedController(val key: String, val name: String)
