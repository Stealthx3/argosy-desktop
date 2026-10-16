package com.nendo.argosy.di

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
actual annotation class HiltViewModel(actual val assistedFactory: KClass<*> = Any::class)
