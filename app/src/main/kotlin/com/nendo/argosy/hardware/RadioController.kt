package com.nendo.argosy.hardware

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.nendo.argosy.util.RootShell
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class RadioStatus(
    val wifiEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val bluetoothEnabled: Boolean = false
)

/**
 * Wi-Fi and Bluetooth state for every device, and switching them through the root route where one
 * exists. Android refuses both switches to ordinary apps, so [canToggle] is false without it.
 * Switching blocks on a shell call.
 */
@Singleton
class RadioController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    val canToggle: Boolean
        get() = RootShell.isAvailable

    fun status(): RadioStatus = RadioStatus(
        wifiEnabled = wifiManager?.isWifiEnabled == true,
        wifiConnected = isWifiConnected(),
        bluetoothEnabled = isBluetoothEnabled()
    )

    fun setWifiEnabled(enabled: Boolean): Boolean {
        val cmdState = if (enabled) "enabled" else "disabled"
        val svcState = if (enabled) "enable" else "disable"
        return runRoot("cmd wifi set-wifi-enabled $cmdState || svc wifi $svcState")
    }

    fun setBluetoothEnabled(enabled: Boolean): Boolean {
        val state = if (enabled) "enable" else "disable"
        return runRoot("cmd bluetooth_manager $state || svc bluetooth $state")
    }

    private fun runRoot(script: String): Boolean = RootShell.run(context, script)?.succeeded == true

    private fun isWifiConnected(): Boolean = try {
        val manager = connectivityManager ?: return false
        manager.getNetworkCapabilities(manager.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    } catch (_: SecurityException) {
        false
    }

    private fun isBluetoothEnabled(): Boolean = try {
        bluetoothManager?.adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }
}
