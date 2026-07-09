package com.example.fyp_hotspot_mobility.viewmodel

import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity

data class HotspotUiState(
    val ssid: String? = null,
    val isHotspotEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val devices: List<ConnectedDevice> = emptyList(),
    val hasScannedAtLeastOnce: Boolean = false,
    val limitExceededAlert: String? = null,
    val selectedDeviceLogs: List<UsageLogEntity> = emptyList()
) {
    val connectedCount: Int get() = devices.count { !it.isBlocked }
}
