package com.example.fyp_hotspot_mobility.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fyp_hotspot_mobility.data.local.dao.DeviceDao
import com.example.fyp_hotspot_mobility.data.local.entity.DeviceEntity
import com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity

@Database(
    entities = [
        DeviceEntity::class,
        UsageLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hotspot_mobility_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
