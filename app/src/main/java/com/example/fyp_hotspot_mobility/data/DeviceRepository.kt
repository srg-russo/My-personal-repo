package com.example.fyp_hotspot_mobility.data

import android.content.Context
import com.example.fyp_hotspot_mobility.data.local.AppDatabase
import com.example.fyp_hotspot_mobility.data.local.entity.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DeviceRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.deviceDao()

    suspend fun isBlocked(deviceId: String): Boolean {
        return dao.getDeviceByMac(deviceId)?.isBlocked ?: false
    }

    suspend fun blockDevice(deviceId: String) {
        val device = dao.getDeviceByMac(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(isBlocked = true))
        } else {
            dao.insertDevice(DeviceEntity(macAddress = deviceId, name = null, ipAddress = "", isBlocked = true))
        }
    }

    suspend fun unblockDevice(deviceId: String) {
        val device = dao.getDeviceByMac(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(isBlocked = false))
        }
    }

    suspend fun getNickname(deviceId: String): String? {
        return dao.getDeviceByMac(deviceId)?.name
    }

    suspend fun setNickname(deviceId: String, nickname: String) {
        val device = dao.getDeviceByMac(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(name = nickname))
        } else {
            dao.insertDevice(DeviceEntity(macAddress = deviceId, name = nickname, ipAddress = ""))
        }
    }

    suspend fun saveIpMapping(ip: String, uniqueId: String) {
        val device = dao.getDeviceByMac(uniqueId)
        if (device != null) {
            dao.updateDevice(device.copy(ipAddress = ip))
        } else {
            dao.insertDevice(DeviceEntity(macAddress = uniqueId, name = null, ipAddress = ip))
        }
    }

    suspend fun getUniqueIdForIp(ip: String): String? {
        return dao.getMacByIp(ip)
    }

    suspend fun getDataLimit(deviceId: String): Int? {
        return dao.getDeviceByMac(deviceId)?.dataLimitMb
    }

    suspend fun setDataLimit(deviceId: String, limitMb: Int?) {
        val device = dao.getDeviceByMac(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(dataLimitMb = limitMb))
        }
    }

    suspend fun getUsage(deviceId: String): Float {
        return dao.getDeviceByMac(deviceId)?.usageMb ?: 0f
    }

    suspend fun addUsage(deviceId: String, amountMb: Float) {
        val device = dao.getDeviceByMac(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(usageMb = device.usageMb + amountMb))
        }
    }

    suspend fun resetUsage(deviceId: String) {
        val device = dao.getDeviceByMac(deviceId)
        if (device != null) {
            dao.updateDevice(device.copy(usageMb = 0f))
        }
    }

    suspend fun resetAllUsage() {
        dao.resetAllUsage()
    }

    suspend fun archiveAndResetUsage() {
        val devices = dao.getAllDevices().first()
        for (device in devices) {
            if (device.usageMb > 0) {
                dao.insertUsageLog(
                    com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity(
                        deviceMac = device.macAddress,
                        usageMb = device.usageMb
                    )
                )
            }
        }
        dao.resetAllUsage()
    }

    fun getUsageLogs(mac: String): Flow<List<com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity>> {
        return dao.getUsageLogsForDevice(mac)
    }

    suspend fun clearUsageLogs(mac: String) {
        dao.deleteUsageLogsForDevice(mac)
    }
}
