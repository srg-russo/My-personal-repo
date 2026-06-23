package com.example.fyp_hotspot_mobility.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log

class WifiStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
            val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
            
            when (state) {
                WifiManager.WIFI_STATE_ENABLED -> {
                    Log.d("WifiStateReceiver", "Wi-Fi Toggled ON. Starting Agent Service...")
                    val serviceIntent = Intent(context, AgentService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                WifiManager.WIFI_STATE_DISABLED -> {
                    Log.d("WifiStateReceiver", "Wi-Fi Toggled OFF. Stopping Agent Service...")
                    val serviceIntent = Intent(context, AgentService::class.java)
                    context.stopService(serviceIntent)
                }
            }
        }
    }
}
