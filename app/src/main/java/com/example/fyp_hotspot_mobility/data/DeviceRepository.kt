package com.example.fyp_hotspot_mobility.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple persistence layer for blocked devices, nicknames, and bandwidth caps.
 */
class DeviceRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("hotspot_manager_prefs", Context.MODE_PRIVATE)

    private val _blockedDeviceIds = MutableStateFlow(loadBlockedIds())
    val blockedDeviceIds: StateFlow<Set<String>> = _blockedDeviceIds.asStateFlow()

    fun isBlocked(deviceId: String): Boolean = _blockedDeviceIds.value.contains(deviceId)

    fun blockDevice(deviceId: String) {
        val updated = _blockedDeviceIds.value.toMutableSet().also { it.add(deviceId) }
        _blockedDeviceIds.value = updated
        persistBlockedIds(updated)
    }

    fun unblockDevice(deviceId: String) {
        val updated = _blockedDeviceIds.value.toMutableSet().also { it.remove(deviceId) }
        _blockedDeviceIds.value = updated
        persistBlockedIds(updated)
    }

    private fun loadBlockedIds(): Set<String> {
        return prefs.getStringSet("blocked_device_ids", emptySet()) ?: emptySet()
    }

    private fun persistBlockedIds(ids: Set<String>) {
        prefs.edit().putStringSet("blocked_device_ids", ids).apply()
    }

    fun getNickname(deviceId: String): String? {
        return prefs.getString("nickname_$deviceId", null)
    }

    fun setNickname(deviceId: String, nickname: String) {
        prefs.edit().putString("nickname_$deviceId", nickname).apply()
    }

    fun getDataLimit(deviceId: String): Int? {
        return prefs.getInt("data_limit_${deviceId}", -1).let { if (it < 0) null else it }
    }

    fun setDataLimit(deviceId: String, limitMb: Int?) {
        prefs.edit().apply {
            if (limitMb == null) remove("data_limit_${deviceId}")
            else putInt("data_limit_${deviceId}", limitMb)
        }.apply()
    }

    fun getUsage(deviceId: String): Float {
        return prefs.getFloat("usage_${deviceId}", 0f)
    }

    fun addUsage(deviceId: String, amountMb: Float) {
        val current = getUsage(deviceId)
        prefs.edit().putFloat("usage_${deviceId}", current + amountMb).apply()
    }

    fun resetUsage(deviceId: String) {
        prefs.edit().remove("usage_${deviceId}").apply()
    }
}
