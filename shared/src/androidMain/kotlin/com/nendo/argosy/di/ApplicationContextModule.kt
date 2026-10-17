package com.nendo.argosy.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SharedApplicationContextModule {
    @Provides
    @ApplicationContext
    fun applicationContext(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context
    ): Context = context
}
