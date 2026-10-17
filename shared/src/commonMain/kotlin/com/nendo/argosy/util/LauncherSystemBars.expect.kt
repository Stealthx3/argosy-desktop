package com.nendo.argosy.util

import com.nendo.argosy.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

expect class LauncherSystemBars {
    fun follow(scope: CoroutineScope, preferences: Flow<UserPreferences>)
}
