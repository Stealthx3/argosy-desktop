package com.nendo.argosy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
expect fun rememberBatteryState(): State<BatteryState>

@Composable
expect fun rememberNetworkLink(): State<NetworkLink>
