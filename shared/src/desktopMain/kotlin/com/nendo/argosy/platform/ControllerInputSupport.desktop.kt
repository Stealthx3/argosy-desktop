package com.nendo.argosy.platform

actual object ControllerInputSupport {
    actual val modeConfigurable: Boolean = true

    actual val steamInputAvailable: Boolean =
        System.getenv(STEAM_ALLOW_VIRTUAL_GAMEPAD) == "1" ||
            !System.getenv(STEAM_VIRTUAL_GAMEPAD_INFO).isNullOrBlank()

    private const val STEAM_ALLOW_VIRTUAL_GAMEPAD = "SDL_GAMECONTROLLER_ALLOW_STEAM_VIRTUAL_GAMEPAD"
    private const val STEAM_VIRTUAL_GAMEPAD_INFO = "SteamVirtualGamepadInfo"
}
