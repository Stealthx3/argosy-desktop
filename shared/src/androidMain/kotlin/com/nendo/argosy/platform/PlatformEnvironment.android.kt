package com.nendo.argosy.platform

import android.os.Environment
import java.io.File

actual object PlatformEnvironment {
    actual val MEDIA_MOUNTED: String get() = Environment.MEDIA_MOUNTED
    actual val DIRECTORY_DOWNLOADS: String get() = Environment.DIRECTORY_DOWNLOADS

    actual fun getExternalStorageDirectory(): File = Environment.getExternalStorageDirectory()
    actual fun getExternalStoragePublicDirectory(type: String): File =
        Environment.getExternalStoragePublicDirectory(type)
    actual fun getExternalStorageState(): String = Environment.getExternalStorageState()
    actual fun isExternalStorageManager(): Boolean = Environment.isExternalStorageManager()
}
