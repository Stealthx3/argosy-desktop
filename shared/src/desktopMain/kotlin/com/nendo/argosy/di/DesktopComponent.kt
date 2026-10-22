package com.nendo.argosy.di

import com.nendo.argosy.data.local.dao.PlatformDao
import com.nendo.argosy.data.preferences.SyncPreferencesRepository
import com.nendo.argosy.platform.PlatformApplication
import com.nendo.argosy.ui.components.LaunchProgressEntryPoint
import com.nendo.argosy.ui.components.recap.MonthlyRecapController
import dagger.BindsInstance
import dagger.Component
import java.io.File
import javax.inject.Singleton

@Singleton
@Component(modules = [DesktopDataModule::class, DesktopAppModule::class])
interface DesktopComponent : LaunchProgressEntryPoint {

    fun platformDao(): PlatformDao

    fun syncPreferences(): SyncPreferencesRepository

    fun monthlyRecapController(): MonthlyRecapController

    fun viewModelComponentFactory(): DesktopViewModelComponent.Factory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance @DataDirectory dataDirectory: File,
            @BindsInstance application: PlatformApplication,
        ): DesktopComponent
    }
}
