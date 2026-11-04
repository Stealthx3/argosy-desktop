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

    private val _confirmOpen = MutableStateFlow(false)
    val confirmOpen: StateFlow<Boolean> = _confirmOpen.asStateFlow()

    fun requestConfirm() {
        _confirmOpen.value = true
    }

    fun dismissConfirm() {
        _confirmOpen.value = false
    }

    fun request() {
        _confirmOpen.value = false
        _requested.value = true
    }
}
