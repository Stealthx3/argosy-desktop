package com.nendo.argosy.libretro

expect class LibretroActivity {
    companion object {
        val EXTRA_ROM_PATH: String

        val EXTRA_CORE_PATH: String

        val EXTRA_SYSTEM_DIR: String

        val EXTRA_GAME_NAME: String

        val EXTRA_GAME_ID: String

        val EXTRA_VARIANT_FILE_ID: String

        val EXTRA_DISC_M3U_PATH: String

        val EXTRA_PLATFORM_SLUG: String

        val EXTRA_CORE_NAME: String

        val EXTRA_CORE_VAR_KEYS: String

        val EXTRA_CORE_VAR_VALUES: String

        val EXTRA_PREFER_VULKAN: String

        val EXTRA_VULKAN_DRIVER_DIR: String

        val EXTRA_VULKAN_DRIVER_LIBRARY: String

        val EXTRA_SAVES_DIR: String

        val EXTRA_STATES_DIR: String

        val EXTRA_NETPLAY_JOIN_SESSION_ID: String

        val EXTRA_NETPLAY_JOIN_HOST_USER_ID: String

        val EXTRA_LAUNCH_CONTROLLER_ID: String

        val ACTION_SHOW_MENU: String

        val ACTION_QUIT: String
    }

    fun enterTouchEditMode()

    fun dispatchTouchKey(action: Int, keyCode: Int)
}
