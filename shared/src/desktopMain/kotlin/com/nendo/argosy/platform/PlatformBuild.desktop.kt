package com.nendo.argosy.platform

actual object PlatformBuild {
    actual val MODEL: String get() = System.getProperty("os.name") ?: "Linux"
    actual val MANUFACTURER: String get() = "desktop"

    actual object VERSION {
        actual val SDK_INT: Int get() = 35
    }

    actual object VERSION_CODES {
        actual val Q: Int get() = 29
        actual val R: Int get() = 30
        actual val S: Int get() = 31
        actual val TIRAMISU: Int get() = 33
    }
}
