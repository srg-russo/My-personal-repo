package com.example.fyp_hotspot_mobility.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "discovery_logs",
    foreignKeys = [
        ForeignKey(
            entity = BluetoothProfileEntity::class,
            parentColumns = ["btAddress"],
            childColumns = ["deviceAddress"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DiscoveryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val timestamp: Long,
    val rssi: Int,
    val isHotspotActive: Boolean,
    val totalTrafficAtTime: Long
)
