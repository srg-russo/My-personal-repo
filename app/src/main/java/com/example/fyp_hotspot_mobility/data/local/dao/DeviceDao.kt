package com.example.fyp_hotspot_mobility.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fyp_hotspot_mobility.data.local.entity.DeviceEntity
import com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE macAddress = :mac")
    suspend fun getDeviceByMac(mac: String): DeviceEntity?

    @Query("SELECT macAddress FROM devices WHERE ipAddress = :ip LIMIT 1")
    suspend fun getMacByIp(ip: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("UPDATE devices SET isBlocked = :blocked WHERE macAddress = :mac")
    suspend fun updateBlockedStatus(mac: String, blocked: Boolean)

    @Query("UPDATE devices SET usageMb = 0")
    suspend fun resetAllUsage()

    @Insert
    suspend fun insertUsageLog(log: UsageLogEntity)

    @Query("SELECT * FROM usage_logs WHERE deviceMac = :mac ORDER BY timestamp DESC")
    fun getUsageLogsForDevice(mac: String): Flow<List<UsageLogEntity>>

    @Query("DELETE FROM usage_logs WHERE deviceMac = :mac")
    suspend fun deleteUsageLogsForDevice(mac: String)
}
