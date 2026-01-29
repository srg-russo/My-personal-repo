package com.example.fyp_hotspot_mobility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple in-memory session manager for per-device data limits and usage.
 * - Limits and usage are kept in memory for the app session.
 * - The manager exposes StateFlow maps so UI can observe changes.
 * Note: collecting real per-client traffic requires platform support and is outside
 * the scope of this module. Use `recordUsage` to update observed data usage.
 */
object DataLimitManager {
    private val _limits = MutableStateFlow<Map<String, Long>>(emptyMap())
    val limitsFlow: StateFlow<Map<String, Long>> get() = _limits

    private val _usage = MutableStateFlow<Map<String, Long>>(emptyMap())
    val usageFlow: StateFlow<Map<String, Long>> get() = _usage

    fun setLimit(mac: String, bytes: Long) {
        _limits.value = _limits.value.toMutableMap().also { it[mac] = bytes }
    }

    fun removeLimit(mac: String) {
        _limits.value = _limits.value.toMutableMap().also { it.remove(mac) }
    }

    fun getLimit(mac: String): Long? = _limits.value[mac]

    fun recordUsage(mac: String, bytes: Long) {
        val current = _usage.value[mac] ?: 0L
        _usage.value = _usage.value.toMutableMap().also { it[mac] = current + bytes }
    }

    fun setUsage(mac: String, bytes: Long) {
        _usage.value = _usage.value.toMutableMap().also { it[mac] = bytes }
    }

    fun getUsage(mac: String): Long = _usage.value[mac] ?: 0L

    fun isOverLimit(mac: String): Boolean {
        val limit = getLimit(mac) ?: return false
        return getUsage(mac) >= limit
    }

    fun clearSession() {
        _limits.value = emptyMap()
        _usage.value = emptyMap()
    }
}
