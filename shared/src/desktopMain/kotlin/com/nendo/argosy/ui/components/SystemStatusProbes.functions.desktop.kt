package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private const val PROBE_INTERVAL_MS = 15_000L

@Composable
actual fun rememberBatteryState(): State<BatteryState> {
    val state = remember { mutableStateOf(BatteryState()) }
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) { readBattery() }?.let { state.value = it }
            delay(PROBE_INTERVAL_MS)
        }
    }
    return state
}

@Composable
actual fun rememberNetworkLink(): State<NetworkLink> {
    val state = remember { mutableStateOf(NetworkLink.OFFLINE) }
    LaunchedEffect(Unit) {
        while (true) {
            state.value = withContext(Dispatchers.IO) { readNetworkLink() }
            delay(PROBE_INTERVAL_MS)
        }
    }
    return state
}

private fun File.text(): String? = runCatching { readText().trim() }.getOrNull()

private fun readBattery(): BatteryState? {
    val supplies = File("/sys/class/power_supply").listFiles().orEmpty()
    val battery = supplies.firstOrNull { File(it, "type").text() == "Battery" } ?: return null
    val level = File(battery, "capacity").text()?.toIntOrNull() ?: return null
    val status = File(battery, "status").text()
    return BatteryState(level = level.coerceIn(0, 100), isCharging = status == "Charging" || status == "Full")
}

private fun readNetworkLink(): NetworkLink {
    val up = File("/sys/class/net").listFiles().orEmpty()
        .filter { it.name != "lo" && File(it, "operstate").text() == "up" }
    return when {
        up.any { File(it, "type").text() == "1" && !it.name.startsWith("wwan") && !File(it, "wireless").exists() && File(it, "device").exists() } -> NetworkLink.ETHERNET
        up.any { File(it, "wireless").exists() } -> NetworkLink.WIFI
        up.any { it.name.startsWith("wwan") } -> NetworkLink.CELLULAR
        else -> NetworkLink.OFFLINE
    }
}
