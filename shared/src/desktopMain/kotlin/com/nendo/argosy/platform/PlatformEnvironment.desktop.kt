package com.nendo.argosy.platform

import java.io.File

actual object PlatformEnvironment {
    actual val MEDIA_MOUNTED: String = "mounted"
    actual val DIRECTORY_DOWNLOADS: String = "Download"
    actual val PRIMARY_STORAGE_LABEL: String = "~"

    actual fun getExternalStorageDirectory(): File = File(System.getProperty("user.home"))
    actual fun getExternalStoragePublicDirectory(type: String): File =
        if (type == DIRECTORY_DOWNLOADS) {
            XdgUserDirs.download()
        } else {
            getExternalStorageDirectory().resolve(type)
        }
    actual fun getExternalStorageState(): String = MEDIA_MOUNTED
    actual fun isExternalStorageManager(): Boolean = true
}
