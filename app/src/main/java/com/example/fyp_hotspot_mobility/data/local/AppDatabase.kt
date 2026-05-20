package com.example.fyp_hotspot_mobility.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fyp_hotspot_mobility.data.local.dao.BluetoothDao
import com.example.fyp_hotspot_mobility.data.local.dao.DeviceDao
import com.example.fyp_hotspot_mobility.data.local.entity.BluetoothProfileEntity
import com.example.fyp_hotspot_mobility.data.local.entity.DeviceEntity
import com.example.fyp_hotspot_mobility.data.local.entity.DiscoveryLogEntity
import com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity

@Database(
    entities = [
        DeviceEntity::class,
        UsageLogEntity::class,
        BluetoothProfileEntity::class,
        DiscoveryLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun bluetoothDao(): BluetoothDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hotspot_mobility_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
