package com.nendo.argosy.core.input

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class ConnectedControllerTracker @Inject constructor() {
    actual val connectedControllerIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
}
