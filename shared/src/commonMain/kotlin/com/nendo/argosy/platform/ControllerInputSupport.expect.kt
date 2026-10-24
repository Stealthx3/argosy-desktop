package com.nendo.argosy.platform

/**
 * What this platform lets the user choose about controller input. [modeConfigurable] is true where
 * pads are read through SDL3 and a ControllerInputMode applies; [steamInputAvailable] is true while
 * Steam Input presents a virtual gamepad to this process.
 */
expect object ControllerInputSupport {
    val modeConfigurable: Boolean
    val steamInputAvailable: Boolean
}
