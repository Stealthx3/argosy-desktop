package com.nendo.argosy.data.download

enum class ThermalState { NORMAL, THROTTLED, PAUSED }

data class ThermalStatus(
    val state: ThermalState = ThermalState.NORMAL,
    val cpuTemp: Float = 0f,
    val batteryTemp: Float = 0f,
    val throttleMultiplier: Float = 1.0f
)
