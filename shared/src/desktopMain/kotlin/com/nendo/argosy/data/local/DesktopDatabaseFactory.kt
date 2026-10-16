package com.nendo.argosy.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

object DesktopDatabaseFactory {

    fun create(file: File): ALauncherDatabase =
        Room.databaseBuilder<ALauncherDatabase>(name = file.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
}
