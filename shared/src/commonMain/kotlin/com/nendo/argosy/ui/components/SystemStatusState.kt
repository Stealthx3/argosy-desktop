package com.nendo.argosy.ui.components

import androidx.compose.runtime.getValue

data class BatteryState(
    val level: Int = 100,
    val isCharging: Boolean = false
)

enum class NetworkLink { OFFLINE, WIFI, ETHERNET, CELLULAR }
