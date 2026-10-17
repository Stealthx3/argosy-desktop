package com.nendo.argosy.platform

import java.io.File

expect object PlatformEnvironment {
    val MEDIA_MOUNTED: String
    val DIRECTORY_DOWNLOADS: String

    fun getExternalStorageDirectory(): File
    fun getExternalStoragePublicDirectory(type: String): File
    fun getExternalStorageState(): String
    fun isExternalStorageManager(): Boolean
}
