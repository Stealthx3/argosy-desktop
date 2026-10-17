package com.nendo.argosy.util

import com.nendo.argosy.data.preferences.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

actual class LauncherSystemBars @Inject constructor() {
    actual fun follow(scope: CoroutineScope, preferences: Flow<UserPreferences>) {
    }
}
