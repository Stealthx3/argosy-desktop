package com.nendo.argosy.platform

import android.os.Build

actual object PlatformBuild {
    actual val MODEL: String get() = Build.MODEL
    actual val MANUFACTURER: String get() = Build.MANUFACTURER

    actual object VERSION {
        actual val SDK_INT: Int get() = Build.VERSION.SDK_INT
    }

    actual object VERSION_CODES {
        actual val Q: Int get() = Build.VERSION_CODES.Q
        actual val R: Int get() = Build.VERSION_CODES.R
        actual val S: Int get() = Build.VERSION_CODES.S
        actual val TIRAMISU: Int get() = Build.VERSION_CODES.TIRAMISU
    }
}
