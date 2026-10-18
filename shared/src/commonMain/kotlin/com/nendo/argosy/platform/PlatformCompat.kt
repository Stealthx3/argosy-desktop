package com.nendo.argosy.platform

expect open class PlatformActivityNotFoundException : RuntimeException

expect class PlatformDisplay

expect val PlatformDisplay.displayId: Int

expect object PlatformContextCompat {
    fun getDisplayOrDefault(context: PlatformContext): PlatformDisplay

    fun checkSelfPermission(context: PlatformContext, permission: String): Int
}

object PlatformPackageManagerConstants {
    const val PERMISSION_GRANTED: Int = 0
    const val PERMISSION_DENIED: Int = -1
}

object PlatformManifest {
    object permission {
        const val BLUETOOTH_ADVERTISE: String = "android.permission.BLUETOOTH_ADVERTISE"
        const val BLUETOOTH_CONNECT: String = "android.permission.BLUETOOTH_CONNECT"
        const val BLUETOOTH_SCAN: String = "android.permission.BLUETOOTH_SCAN"
        const val CAMERA: String = "android.permission.CAMERA"
        const val POST_NOTIFICATIONS: String = "android.permission.POST_NOTIFICATIONS"
        const val READ_EXTERNAL_STORAGE: String = "android.permission.READ_EXTERNAL_STORAGE"
        const val READ_MEDIA_IMAGES: String = "android.permission.READ_MEDIA_IMAGES"
        const val WRITE_EXTERNAL_STORAGE: String = "android.permission.WRITE_EXTERNAL_STORAGE"
    }
}
