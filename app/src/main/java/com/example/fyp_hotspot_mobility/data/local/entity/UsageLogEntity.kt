package com.example.fyp_hotspot_mobility.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_logs",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["macAddress"],
            childColumns = ["deviceMac"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deviceMac"])]
)
data class UsageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceMac: String,
    val usageMb: Float,
    val timestamp: Long = System.currentTimeMillis()
)
