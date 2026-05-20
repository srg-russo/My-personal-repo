package com.example.fyp_hotspot_mobility.viewmodel

import com.example.fyp_hotspot_mobility.data.BluetoothScanner
import com.example.fyp_hotspot_mobility.model.ConnectedDevice

// UI state holder for the hotspot manager.
data class HotspotUiState(
    val ssid: String? = null,
    val isHotspotEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val devices: List<ConnectedDevice> = emptyList(),
    val nearbyBluetoothDevices: List<BluetoothScanner.DiscoveredBluetoothDevice> = emptyList(),
    val hasScannedAtLeastOnce: Boolean = false,
    val limitExceededAlert: String? = null
) {
    val connectedCount: Int get() = devices.count { !it.isBlocked }
}
