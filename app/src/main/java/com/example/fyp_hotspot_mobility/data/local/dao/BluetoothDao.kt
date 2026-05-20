package com.example.fyp_hotspot_mobility.data.local.dao

import androidx.room.*
import com.example.fyp_hotspot_mobility.data.local.entity.BluetoothProfileEntity
import com.example.fyp_hotspot_mobility.data.local.entity.DiscoveryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothDao {
    @Query("SELECT * FROM bluetooth_profiles ORDER BY lastSeen DESC")
    fun getAllProfiles(): Flow<List<BluetoothProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BluetoothProfileEntity)

    @Update
    suspend fun updateProfile(profile: BluetoothProfileEntity)

    @Query("SELECT * FROM bluetooth_profiles WHERE btAddress = :address")
    suspend fun getProfileByAddress(address: String): BluetoothProfileEntity?

    @Insert
    suspend fun insertDiscoveryLog(log: DiscoveryLogEntity)

    @Query("SELECT * FROM discovery_logs WHERE deviceAddress = :address ORDER BY timestamp DESC")
    fun getLogsForDevice(address: String): Flow<List<DiscoveryLogEntity>>

    @Query("DELETE FROM discovery_logs WHERE timestamp < :threshold")
    suspend fun deleteOldLogs(threshold: Long)
}
