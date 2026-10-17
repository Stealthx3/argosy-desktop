package com.nendo.argosy.platform

import java.io.File

private val arch: String = System.getProperty("os.arch")

private val desktopAbi: String = when (arch) {
    "amd64", "x86_64" -> "x86_64"
    "aarch64", "arm64" -> "arm64-v8a"
    else -> arch
}

actual object PlatformBuild {
    actual val MODEL: String get() = System.getProperty("os.name") ?: "Linux"
    actual val MANUFACTURER: String get() = "desktop"
    actual val BRAND: String get() = "desktop"
    actual val DEVICE: String get() = "desktop"
    actual val PRODUCT: String get() = "argosy-desktop"
    actual val BOARD: String get() = arch
    actual val FINGERPRINT: String get() = "desktop/${System.getProperty("os.name")}/${System.getProperty("os.version")}"
    actual val SUPPORTED_64_BIT_ABIS: Array<String> get() = if (PlatformProcess.is64Bit()) arrayOf(desktopAbi) else emptyArray()
    actual val SUPPORTED_32_BIT_ABIS: Array<String> get() = if (PlatformProcess.is64Bit()) emptyArray() else arrayOf(desktopAbi)

    actual object VERSION {
        actual val SDK_INT: Int get() = 35
        actual val RELEASE: String get() = System.getProperty("os.version") ?: ""
    }

    actual object VERSION_CODES {
        actual val N: Int get() = 24
        actual val O: Int get() = 26
        actual val P: Int get() = 28
        actual val Q: Int get() = 29
        actual val R: Int get() = 30
        actual val S: Int get() = 31
        actual val TIRAMISU: Int get() = 33
        actual val UPSIDE_DOWN_CAKE: Int get() = 34
    }
}

actual object PlatformProcess {
    actual fun is64Bit(): Boolean = arch.contains("64")

    actual fun myUid(): Int = runCatching {
        File("/proc/self/status").readLines()
            .first { it.startsWith("Uid:") }
            .split(Regex("\\s+"))[1]
            .toInt()
    }.getOrDefault(-1)
}
