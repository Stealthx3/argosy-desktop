package com.nendo.argosy.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBatteryState(): State<BatteryState> {
    val context = LocalContext.current
    val batteryState = remember { mutableStateOf(BatteryState()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                batteryState.value = BatteryState(
                    level = (level * 100) / scale,
                    isCharging = isCharging
                )
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = context.registerReceiver(receiver, filter)

        sticky?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            batteryState.value = BatteryState(
                level = (level * 100) / scale,
                isCharging = isCharging
            )
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return batteryState
}

@Composable
actual fun rememberNetworkLink(): State<NetworkLink> {
    val context = LocalContext.current
    val link = remember { mutableStateOf(NetworkLink.OFFLINE) }

    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                link.value = caps.toNetworkLink()
            }

            override fun onLost(network: Network) {
                link.value = NetworkLink.OFFLINE
            }
        }
        link.value = manager
            ?.let { it.getNetworkCapabilities(it.activeNetwork) }
            .toNetworkLink()
        manager?.registerDefaultNetworkCallback(callback)

        onDispose { manager?.unregisterNetworkCallback(callback) }
    }

    return link
}

private fun NetworkCapabilities?.toNetworkLink(): NetworkLink {
    if (this == null || !hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        return NetworkLink.OFFLINE
    }
    return when {
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkLink.ETHERNET
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkLink.WIFI
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkLink.CELLULAR
        else -> NetworkLink.OFFLINE
    }
}
