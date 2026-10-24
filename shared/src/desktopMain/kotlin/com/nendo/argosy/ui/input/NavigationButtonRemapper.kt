package com.nendo.argosy.ui.input

import com.nendo.argosy.data.preferences.ControlsPreferencesRepository
import com.nendo.argosy.data.preferences.NavigationButtonMaps
import com.nendo.argosy.util.SafeCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The stored navigation remaps applied to desktop key and gamepad presses before they reach
 * [GamepadInputHandler]. While a capture is armed, the next press is handed to it unmapped and
 * neither its press nor its release reaches the handler.
 */
@Singleton
class NavigationButtonRemapper @Inject constructor(
    controlsPreferences: ControlsPreferencesRepository,
) {
    @Volatile
    private var maps = NavigationButtonMaps()
    private var capture: ((controller: String, keyCode: Int) -> Unit)? = null
    private val swallowed = mutableSetOf<Pair<String, Int>>()
    private val scope = SafeCoroutineScope(Dispatchers.Default, TAG)

    init {
        scope.launch { controlsPreferences.preferences.collect { maps = it.navigationButtonMaps } }
    }

    fun translate(controller: String, keyCode: Int): Int = maps.translate(controller, keyCode)

    fun captureNext(listener: (controller: String, keyCode: Int) -> Unit) {
        capture = listener
    }

    fun cancelCapture() {
        capture = null
    }

    fun interceptDown(controller: String, keyCode: Int): Boolean {
        val listener = capture ?: return false
        capture = null
        swallowed += controller to keyCode
        listener(controller, keyCode)
        return true
    }

    fun isSwallowed(controller: String, keyCode: Int): Boolean = (controller to keyCode) in swallowed

    fun interceptUp(controller: String, keyCode: Int): Boolean = swallowed.remove(controller to keyCode)

    companion object {
        private const val TAG = "NavigationButtonRemapper"

        fun controllerKey(descriptor: String): String = descriptor.substringBefore(':')
    }
}
