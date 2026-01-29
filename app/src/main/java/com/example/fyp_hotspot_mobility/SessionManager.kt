package com.example.fyp_hotspot_mobility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks first-seen timestamps for devices during the app session.
 */
object SessionManager {
    private val _sessionStarts = MutableStateFlow<Map<String, Long>>(emptyMap())
    val sessionStartsFlow: StateFlow<Map<String, Long>> get() = _sessionStarts

    fun recordSeen(mac: String) {
        if (_sessionStarts.value.containsKey(mac)) return
        _sessionStarts.value = _sessionStarts.value.toMutableMap().also { it[mac] = System.currentTimeMillis() }
    }

    fun getSessionStart(mac: String): Long? = _sessionStarts.value[mac]

    fun getSessionDurationMillis(mac: String): Long {
        val start = getSessionStart(mac) ?: return 0L
        return System.currentTimeMillis() - start
    }

    fun clear() {
        _sessionStarts.value = emptyMap()
    }
}
