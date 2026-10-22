package com.nendo.argosy.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Provider

@Subcomponent(modules = [DesktopViewModelModule::class])
interface DesktopViewModelComponent {

    fun viewModels(): Map<Class<*>, @JvmSuppressWildcards Provider<ViewModel>>

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance savedStateHandle: SavedStateHandle): DesktopViewModelComponent
    }
}
