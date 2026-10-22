package com.nendo.argosy.ui.components

import android.content.Context
import com.nendo.argosy.data.emulator.LaunchProgressTracker
import com.nendo.argosy.ui.components.recap.LaunchRecapCursor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LaunchProgressHiltEntryPoint : LaunchProgressEntryPoint {
    override fun launchProgressTracker(): LaunchProgressTracker
    override fun launchRecapCursor(): LaunchRecapCursor
}

actual fun launchProgressEntryPoint(context: Context): LaunchProgressEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, LaunchProgressHiltEntryPoint::class.java)
