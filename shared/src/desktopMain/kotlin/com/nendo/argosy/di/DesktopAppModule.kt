package com.nendo.argosy.di

import com.nendo.argosy.data.storage.DesktopFileAccessLayer
import com.nendo.argosy.data.storage.FileAccessLayer
import com.nendo.argosy.data.sync.SyncNotificationCopyResources
import com.nendo.argosy.domain.usecase.recap.MonthlyRecapOverride
import com.nendo.argosy.domain.usecase.recap.PreLaunchRecapOverride
import com.nendo.argosy.domain.usecase.sync.SyncNotificationCopy
import com.nendo.argosy.hardware.DesktopLEDController
import com.nendo.argosy.hardware.LEDController
import com.nendo.argosy.platform.PlatformApplication
import com.nendo.argosy.platform.PlatformContext
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module(subcomponents = [DesktopViewModelComponent::class])
abstract class DesktopAppModule {

    @Binds
    @Singleton
    abstract fun bindFileAccessLayer(impl: DesktopFileAccessLayer): FileAccessLayer

    @Binds
    abstract fun bindSyncNotificationCopy(impl: SyncNotificationCopyResources): SyncNotificationCopy

    @Binds
    abstract fun bindLEDController(impl: DesktopLEDController): LEDController

    @Binds
    abstract fun bindContext(application: PlatformApplication): PlatformContext

    @Binds
    @ApplicationContext
    abstract fun bindApplicationContext(application: PlatformApplication): PlatformContext

    @Multibinds
    abstract fun preLaunchRecapOverrides(): Set<PreLaunchRecapOverride>

    @Multibinds
    abstract fun monthlyRecapOverrides(): Set<MonthlyRecapOverride>

    companion object {
        @Provides
        @Singleton
        fun provideMoshi(): Moshi = Moshi.Builder().build()
    }
}
