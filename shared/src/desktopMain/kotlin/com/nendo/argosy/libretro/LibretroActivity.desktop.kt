package com.nendo.argosy.libretro

import javax.inject.Inject

actual class LibretroActivity @Inject constructor() {
    actual companion object {
        actual const val EXTRA_ROM_PATH: String = "rom_path"

        actual const val EXTRA_CORE_PATH: String = "core_path"

        actual const val EXTRA_SYSTEM_DIR: String = "system_dir"

        actual const val EXTRA_GAME_NAME: String = "game_name"

        actual const val EXTRA_GAME_ID: String = "game_id"

        actual const val EXTRA_VARIANT_FILE_ID: String = "variant_file_id"

        actual const val EXTRA_DISC_M3U_PATH: String = "disc_m3u_path"

        actual const val EXTRA_PLATFORM_SLUG: String = "platform_slug"

        actual const val EXTRA_CORE_NAME: String = "core_name"

        actual const val EXTRA_CORE_VAR_KEYS: String = "core_var_keys"

        actual const val EXTRA_CORE_VAR_VALUES: String = "core_var_values"

        actual const val EXTRA_PREFER_VULKAN: String = "prefer_vulkan"

        actual const val EXTRA_VULKAN_DRIVER_DIR: String = "vulkan_driver_dir"

        actual const val EXTRA_VULKAN_DRIVER_LIBRARY: String = "vulkan_driver_library"

        actual const val EXTRA_SAVES_DIR: String = "saves_dir"

        actual const val EXTRA_STATES_DIR: String = "states_dir"

        actual const val EXTRA_NETPLAY_JOIN_SESSION_ID: String = "netplay_join_session_id"

        actual const val EXTRA_NETPLAY_JOIN_HOST_USER_ID: String = "netplay_join_host_user_id"

        actual const val EXTRA_LAUNCH_CONTROLLER_ID: String = "launch_controller_id"

        actual const val ACTION_SHOW_MENU: String = "com.nendo.argosy.action.SHOW_MENU"

        actual const val ACTION_QUIT: String = "com.nendo.argosy.action.QUIT"
    }

    actual fun enterTouchEditMode() {
    }

    actual fun dispatchTouchKey(action: Int, keyCode: Int) {
    }
}
