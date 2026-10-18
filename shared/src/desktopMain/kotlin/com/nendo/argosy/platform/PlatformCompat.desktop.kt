package com.nendo.argosy.platform

actual open class PlatformActivityNotFoundException : RuntimeException()

actual class PlatformDisplay(val id: Int)

actual val PlatformDisplay.displayId: Int get() = id

actual object PlatformContextCompat {
    actual fun getDisplayOrDefault(context: PlatformContext): PlatformDisplay = PlatformDisplay(0)

    actual fun checkSelfPermission(context: PlatformContext, permission: String): Int =
        PlatformPackageManagerConstants.PERMISSION_GRANTED
}
