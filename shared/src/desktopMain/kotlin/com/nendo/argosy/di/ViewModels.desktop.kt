package com.nendo.argosy.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.reflect.KClass

val LocalViewModelProviders = staticCompositionLocalOf<Map<KClass<out ViewModel>, () -> ViewModel>> { emptyMap() }

@Composable
actual fun <VM : ViewModel> hiltViewModelOf(modelClass: KClass<VM>): VM {
    val providers = LocalViewModelProviders.current
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            val provider = checkNotNull(providers[modelClass]) { "no desktop provider for ${modelClass.simpleName}" }
            @Suppress("UNCHECKED_CAST")
            return provider() as T
        }
    }
    return viewModel(modelClass = modelClass, factory = factory)
}
