package com.nendo.argosy.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import kotlin.reflect.KClass

@Composable
actual fun <VM : ViewModel> hiltViewModelOf(modelClass: KClass<VM>): VM {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val factory = if (owner is NavBackStackEntry) HiltViewModelFactory(LocalContext.current, owner) else null
    return viewModel(modelClass = modelClass, viewModelStoreOwner = owner, factory = factory)
}
