package com.nendo.argosy.ui.screens.player

import javax.inject.Inject

actual class PlayerEngine @Inject constructor() {
    actual suspend fun authorizationHeader(): String = ""
}
