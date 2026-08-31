package com.example.fyp_hotspot_mobility.data

import android.content.Context
import com.example.fyp_hotspot_mobility.data.local.AppDatabase
import com.example.fyp_hotspot_mobility.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.*

class DeviceRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.deviceDao()

    suspend fun isBlocked(deviceId: String): Boolean {
        return dao.getDeviceByDeviceId(deviceId)?.isBlocked ?: false
    }

    suspend fun blockDevice(deviceId: String) {
        val device = dao.getDeviceByDeviceId(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(isBlocked = true))
        } else {
            dao.insertDevice(DeviceEntity(deviceId = deviceId, name = null, ipAddress = "", isBlocked = true))
        }
    }

    suspend fun unblockDevice(deviceId: String) {
        val device = dao.getDeviceByDeviceId(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(isBlocked = false))
        }
    }

    suspend fun getNickname(deviceId: String): String? {
        return dao.getDeviceByDeviceId(deviceId)?.name
    }

    suspend fun setNickname(deviceId: String, nickname: String) {
        val device = dao.getDeviceByDeviceId(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(name = nickname))
        } else {
            dao.insertDevice(DeviceEntity(deviceId = deviceId, name = nickname, ipAddress = ""))
        }
    }

    suspend fun saveIpMapping(ip: String, deviceId: String) {
        dao.clearStaleIpMapping(ip, deviceId)
        val device = dao.getDeviceByDeviceId(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(ipAddress = ip))
        } else {
            dao.insertDevice(DeviceEntity(deviceId = deviceId, name = null, ipAddress = ip))
        }
    }

    suspend fun migrateNickname(fromId: String, toId: String) {
        val oldDevice = dao.getDeviceByDeviceId(fromId)
        val newDevice = dao.getDeviceByDeviceId(toId)
        
        if (oldDevice != null && !oldDevice.name.isNullOrBlank()) {
            val nickname = oldDevice.name
            if (newDevice != null) {
                dao.updateDevice(newDevice.copy(name = nickname))
            } else {
                dao.insertDevice(DeviceEntity(deviceId = toId, name = nickname, ipAddress = oldDevice.ipAddress))
            }
            if (fromId == oldDevice.ipAddress) {
                dao.deleteDeviceById(fromId)
            }
        }
    }

    suspend fun getUniqueIdForIp(ip: String): String? {
        return dao.getDeviceIdByIp(ip)
    }

    suspend fun getDataLimit(deviceId: String): Int? {
        return dao.getDeviceByDeviceId(deviceId)?.dataLimitMb
    }

    suspend fun setDataLimit(deviceId: String, limitMb: Int?) {
        val device = dao.getDeviceByDeviceId(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(dataLimitMb = limitMb))
        }
    }

    suspend fun getUsage(deviceId: String): Float {
        return dao.getDeviceByDeviceId(deviceId)?.usageMb ?: 0f
    }

    suspend fun addUsage(deviceId: String, amountMb: Float) {
        val device = dao.getDeviceByDeviceId(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(usageMb = device.usageMb + amountMb))
        }
    }

    suspend fun archiveAndResetUsage() {
        val devices = dao.getAllDevices().first()
        for (device in devices) {
            if (device.usageMb > 0) {
                dao.insertUsageLog(
                    com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity(
                        deviceId = device.deviceId,
                        usageMb = device.usageMb
                    )
                )
            }
        }
        dao.resetAllUsage()
    }

    fun getUsageLogs(deviceId: String): Flow<List<com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity>> {
        return dao.getUsageLogsForDevice(deviceId)
    }

    suspend fun clearUsageLogs(deviceId: String) {
        dao.deleteUsageLogsForDevice(deviceId)
    }
}
