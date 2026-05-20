package com.example.fyp_hotspot_mobility.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val macAddress: String,
    val name: String?,
    val ipAddress: String,
    val isBlocked: Boolean = false,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)
