package com.example.fyp_hotspot_mobility.model

// Represents a device currently connected to the host hotspot.

data class ConnectedDevice(
    val id: String,           // MAC address
    val ipAddress: String,
    val hostname: String,
    val isBlocked: Boolean,
    val dataLimitMb: Int?,    // null = unlimited
    val usageMb: Float = 0f
)
