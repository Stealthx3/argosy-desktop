package com.nendo.argosy.platform

expect object PlatformBuild {
    val MODEL: String
    val MANUFACTURER: String

    object VERSION {
        val SDK_INT: Int
        val RELEASE: String
    }

    object VERSION_CODES {
        val Q: Int
        val R: Int
        val S: Int
        val TIRAMISU: Int
    }
}
