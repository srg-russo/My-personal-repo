package com.example.fyp_hotspot_mobility.model

// Represents a device currently connected to the host device hotspot.

data class ConnectedDevice(
    val id: String,           // Unique device identifier (IP or Agent ID)
    val ipAddress: String,
    val hostname: String,
    val isBlocked: Boolean,
    val dataLimitMb: Int?,
    val usageMb: Float = 0f
)
