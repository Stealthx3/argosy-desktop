package com.nendo.argosy.di

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
expect annotation class HiltViewModel(val assistedFactory: KClass<*> = Any::class)
