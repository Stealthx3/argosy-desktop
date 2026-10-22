package com.nendo.argosy.ui.components.recap

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MonthlyRecapEntryPoint {
    fun monthlyRecapController(): MonthlyRecapController
}

actual fun monthlyRecapController(context: Context): MonthlyRecapController =
    EntryPointAccessors.fromApplication(context.applicationContext, MonthlyRecapEntryPoint::class.java)
        .monthlyRecapController()
