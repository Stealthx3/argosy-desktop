package com.nendo.argosy.di

import com.nendo.argosy.data.local.dao.PlatformDao
import com.nendo.argosy.data.preferences.SyncPreferencesRepository
import dagger.BindsInstance
import dagger.Component
import java.io.File
import javax.inject.Singleton

@Singleton
@Component(modules = [DesktopDataModule::class])
interface DesktopComponent {

    fun platformDao(): PlatformDao

    fun syncPreferences(): SyncPreferencesRepository

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance @DataDirectory dataDirectory: File): DesktopComponent
    }
}
