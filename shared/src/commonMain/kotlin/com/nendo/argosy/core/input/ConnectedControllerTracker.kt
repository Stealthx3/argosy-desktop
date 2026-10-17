package com.nendo.argosy.core.input

import kotlinx.coroutines.flow.StateFlow

expect class ConnectedControllerTracker {
    val connectedControllerIds: StateFlow<Set<String>>
}
