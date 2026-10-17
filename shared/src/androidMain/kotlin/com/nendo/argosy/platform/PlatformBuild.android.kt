package com.nendo.argosy.platform

import android.os.Build
import android.os.Process

actual object PlatformBuild {
    actual val MODEL: String get() = Build.MODEL ?: "null"
    actual val MANUFACTURER: String get() = Build.MANUFACTURER ?: "null"
    actual val BRAND: String get() = Build.BRAND ?: "null"
    actual val DEVICE: String get() = Build.DEVICE ?: "null"
    actual val PRODUCT: String get() = Build.PRODUCT ?: "null"
    actual val BOARD: String get() = Build.BOARD ?: "null"
    actual val FINGERPRINT: String get() = Build.FINGERPRINT ?: "null"
    actual val SUPPORTED_64_BIT_ABIS: Array<String> get() = Build.SUPPORTED_64_BIT_ABIS ?: emptyArray()
    actual val SUPPORTED_32_BIT_ABIS: Array<String> get() = Build.SUPPORTED_32_BIT_ABIS ?: emptyArray()

    actual object VERSION {
        actual val SDK_INT: Int get() = Build.VERSION.SDK_INT
        actual val RELEASE: String get() = Build.VERSION.RELEASE ?: "null"
    }

    actual object VERSION_CODES {
        actual val N: Int get() = Build.VERSION_CODES.N
        actual val O: Int get() = Build.VERSION_CODES.O
        actual val P: Int get() = Build.VERSION_CODES.P
        actual val Q: Int get() = Build.VERSION_CODES.Q
        actual val R: Int get() = Build.VERSION_CODES.R
        actual val S: Int get() = Build.VERSION_CODES.S
        actual val TIRAMISU: Int get() = Build.VERSION_CODES.TIRAMISU
        actual val UPSIDE_DOWN_CAKE: Int get() = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
}

actual object PlatformProcess {
    actual fun is64Bit(): Boolean = Process.is64Bit()
    actual fun myUid(): Int = Process.myUid()
}
