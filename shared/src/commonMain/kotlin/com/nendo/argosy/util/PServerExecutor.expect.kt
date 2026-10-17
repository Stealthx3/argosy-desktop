package com.nendo.argosy.util

expect object PServerExecutor {
    val isAvailable: Boolean

    fun execute(cmd: String): Result<String?>

    fun getSystemSetting(key: String, default: Int): Int

    fun setSystemSetting(key: String, value: Int): Boolean

    fun setSystemSetting(key: String, value: String): Boolean

    fun getSystemSettingFloat(key: String, default: Float): Float

    fun setSystemSettingFloat(key: String, value: Float): Boolean
}
