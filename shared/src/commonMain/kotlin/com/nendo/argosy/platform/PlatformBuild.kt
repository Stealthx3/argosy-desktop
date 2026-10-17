package com.nendo.argosy.platform

expect object PlatformBuild {
    val MODEL: String
    val MANUFACTURER: String
    val BRAND: String
    val DEVICE: String
    val PRODUCT: String
    val BOARD: String
    val FINGERPRINT: String
    val SUPPORTED_64_BIT_ABIS: Array<String>
    val SUPPORTED_32_BIT_ABIS: Array<String>

    object VERSION {
        val SDK_INT: Int
        val RELEASE: String
    }

    object VERSION_CODES {
        val N: Int
        val O: Int
        val P: Int
        val Q: Int
        val R: Int
        val S: Int
        val TIRAMISU: Int
        val UPSIDE_DOWN_CAKE: Int
    }
}

expect object PlatformProcess {
    fun is64Bit(): Boolean
    fun myUid(): Int
}
