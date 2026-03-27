package com.example.fyp_hotspot_mobility.model

/**
 * Represents a device currently connected to the host hotspot.
 */
data class ConnectedDevice(
    val id: String,           // MAC address
    val ipAddress: String,
    val hostname: String,
    val isBlocked: Boolean,
    val downloadSpeed: Float, // KB/s
    val uploadSpeed: Float,   // KB/s
    val bandwidthLimitKbps: Int? // null = unlimited
)
