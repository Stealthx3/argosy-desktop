package com.nendo.argosy.ui.screens.player

expect class PlayerEngine {
    suspend fun authorizationHeader(): String
}
