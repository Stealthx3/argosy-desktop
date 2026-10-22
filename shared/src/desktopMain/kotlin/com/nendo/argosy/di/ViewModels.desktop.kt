package com.nendo.argosy.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.reflect.KClass

fun interface DesktopViewModelFactory {
    fun create(modelClass: KClass<out ViewModel>, savedStateHandle: SavedStateHandle): ViewModel
}

val LocalViewModelProviders = staticCompositionLocalOf<DesktopViewModelFactory> {
    DesktopViewModelFactory { modelClass, _ -> error("no desktop view model graph for ${modelClass.simpleName}") }
}

fun DesktopComponent.viewModelFactory(): DesktopViewModelFactory = DesktopViewModelFactory { modelClass, handle ->
    val provider = viewModelComponentFactory().create(handle).viewModels()[modelClass.java]
    checkNotNull(provider) { "no desktop provider for ${modelClass.simpleName}" }.get()
}

@Composable
actual fun <VM : ViewModel> hiltViewModelOf(modelClass: KClass<VM>): VM {
    val providers = LocalViewModelProviders.current
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            val handle = if (extras[SAVED_STATE_REGISTRY_OWNER_KEY] != null && extras[VIEW_MODEL_STORE_OWNER_KEY] != null) {
                extras.createSavedStateHandle()
            } else {
                SavedStateHandle()
            }
            @Suppress("UNCHECKED_CAST")
            return providers.create(modelClass, handle) as T
        }
    }
    return viewModel(modelClass = modelClass, factory = factory)
}
