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

    @Query("SELECT * FROM devices WHERE deviceId = :deviceId")
    suspend fun getDeviceByDeviceId(deviceId: String): DeviceEntity?

    @Query("SELECT deviceId FROM devices WHERE ipAddress = :ip LIMIT 1")
    suspend fun getDeviceIdByIp(ip: String): String?

    @Query("UPDATE devices SET ipAddress = '' WHERE ipAddress = :ip AND deviceId != :deviceId")
    suspend fun clearStaleIpMapping(ip: String, deviceId: String)

    @Query("DELETE FROM devices WHERE deviceId = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("UPDATE devices SET isBlocked = :blocked WHERE deviceId = :deviceId")
    suspend fun updateBlockedStatus(deviceId: String, blocked: Boolean)

    @Query("UPDATE devices SET usageMb = 0")
    suspend fun resetAllUsage()

    @Insert
    suspend fun insertUsageLog(log: UsageLogEntity)

    @Query("SELECT * FROM usage_logs WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getUsageLogsForDevice(deviceId: String): Flow<List<UsageLogEntity>>

    @Query("DELETE FROM usage_logs WHERE deviceId = :deviceId")
    suspend fun deleteUsageLogsForDevice(deviceId: String)
}
