package com.nendo.argosy.di

import com.nendo.argosy.data.emulator.HostSessionWatcher
import com.nendo.argosy.data.local.dao.PlatformDao
import com.nendo.argosy.data.preferences.SyncPreferencesRepository
import com.nendo.argosy.data.remote.romm.RomMConnectionManager
import com.nendo.argosy.data.remote.ssl.UserCertStore
import com.nendo.argosy.platform.PlatformApplication
import com.nendo.argosy.ui.components.LaunchProgressEntryPoint
import com.nendo.argosy.ui.components.recap.MonthlyRecapController
import com.nendo.argosy.ui.input.DesktopGamepadDispatcher
import com.nendo.argosy.ui.input.DesktopKeyDispatcher
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

    fun rommConnectionManager(): RomMConnectionManager

    fun userCertStore(): UserCertStore

    fun keyDispatcher(): DesktopKeyDispatcher

    fun gamepadDispatcher(): DesktopGamepadDispatcher

    fun viewModelComponentFactory(): DesktopViewModelComponent.Factory

    fun hostSessionWatcher(): HostSessionWatcher

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance @DataDirectory dataDirectory: File,
            @BindsInstance application: PlatformApplication,
        ): DesktopComponent
    }
}
