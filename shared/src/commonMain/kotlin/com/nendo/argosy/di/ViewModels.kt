package com.nendo.argosy.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass

@Composable
expect fun <VM : ViewModel> hiltViewModelOf(modelClass: KClass<VM>): VM

@Composable
inline fun <reified VM : ViewModel> hiltViewModel(): VM = hiltViewModelOf(VM::class)
