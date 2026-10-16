package com.nendo.argosy.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nendo.argosy.data.local.ALauncherDatabase
import com.nendo.argosy.data.local.DesktopDatabaseFactory
import com.nendo.argosy.util.Logger
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module(includes = [DesktopDaoModule::class])
object DesktopDataModule {

    @Provides
    @Singleton
    fun database(@DataDirectory dataDirectory: File): ALauncherDatabase =
        DesktopDatabaseFactory.create(File(dataDirectory, "alauncher.db"))

    @Provides
    @Singleton
    fun preferences(@DataDirectory dataDirectory: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { cause ->
                Logger.error("DataStore", "preferences file unreadable, starting from empty", cause)
                emptyPreferences()
            },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ) {
            File(dataDirectory, "datastore/settings.preferences_pb")
        }
}
