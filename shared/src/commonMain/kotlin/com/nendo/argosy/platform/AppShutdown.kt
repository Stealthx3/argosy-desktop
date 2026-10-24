package com.nendo.argosy.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A request from the UI to close the app. The desktop shell observes [requested] and shuts down;
 * Android never offers the request.
 */
object AppShutdown {
    private val _requested = MutableStateFlow(false)
    val requested: StateFlow<Boolean> = _requested.asStateFlow()

    fun request() {
        _requested.value = true
    }
}
