package com.nendo.argosy.util



actual object PServerExecutor {
    actual val isAvailable: Boolean = false

    actual fun execute(cmd: String): Result<String?> = Result.failure(UnsupportedOperationException("not available on desktop"))

    actual fun getSystemSetting(key: String, default: Int): Int = 0

    actual fun setSystemSetting(key: String, value: Int): Boolean = false

    actual fun setSystemSetting(key: String, value: String): Boolean = false

    actual fun getSystemSettingFloat(key: String, default: Float): Float = 0f

    actual fun setSystemSettingFloat(key: String, value: Float): Boolean = false
}
