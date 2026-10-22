package com.nendo.argosy.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.nendo.argosy.di.hiltViewModelOf
import kotlin.reflect.KClass

@Composable
actual fun <VM : ViewModel> launcherViewModelOf(modelClass: KClass<VM>): VM = hiltViewModelOf(modelClass)
