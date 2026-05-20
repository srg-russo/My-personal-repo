package com.example.fyp_hotspot_mobility.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_profiles")
data class BluetoothProfileEntity(
    @PrimaryKey val btAddress: String,
    val name: String?,
    val deviceType: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val confidenceScore: Float = 0.0f,
    val customNickname: String? = null
)
