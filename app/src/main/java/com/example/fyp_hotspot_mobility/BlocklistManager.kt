package com.example.fyp_hotspot_mobility

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BlocklistManager {
    private const val PREFS = "fyp_blocklist_prefs"
    private const val KEY_SET = "blocked_macs"

    private lateinit var ctx: Context

    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blockedFlow: StateFlow<Set<String>> get() = _blocked

    fun init(context: Context) {
        ctx = context.applicationContext
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_SET, emptySet()) ?: emptySet()
        _blocked.value = set
    }

    private fun persist() {
        if (!::ctx.isInitialized) return
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SET, _blocked.value).apply()
    }

    fun block(mac: String) {
        _blocked.value = _blocked.value.toMutableSet().also { it.add(mac) }
        persist()
    }

    fun unblock(mac: String) {
        _blocked.value = _blocked.value.toMutableSet().also { it.remove(mac) }
        persist()
    }

    fun isBlocked(mac: String): Boolean = _blocked.value.contains(mac)
}
