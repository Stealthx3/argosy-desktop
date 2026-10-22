package com.nendo.argosy.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass

@Composable
expect fun <VM : ViewModel> launcherViewModelOf(modelClass: KClass<VM>): VM

@Composable
inline fun <reified VM : ViewModel> launcherViewModel(): VM = launcherViewModelOf(VM::class)
