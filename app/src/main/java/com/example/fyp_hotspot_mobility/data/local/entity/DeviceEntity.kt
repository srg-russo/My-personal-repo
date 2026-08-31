package com.example.fyp_hotspot_mobility.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val deviceId: String,
    val name: String?,
    val ipAddress: String,
    val isBlocked: Boolean = false,
    val dataLimitMb: Int? = null,
    val usageMb: Float = 0f,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)
