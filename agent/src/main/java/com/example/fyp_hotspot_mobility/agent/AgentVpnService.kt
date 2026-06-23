package com.example.fyp_hotspot_mobility.agent

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class AgentVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
                .setSession("Hostwatch Block")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDisallowedApplication(packageName) // Excludes the command listener (companion app) from the block
                .setBlocking(true)      // Ensure traffic is blocked
            
            vpnInterface = builder.establish()
            Log.d("AgentVpnService", "VPN Started - Traffic blocked")
            if (vpnInterface == null) {
                Log.e("AgentVpnService", "Failed to establish VPN interface - User might have denied permission")
            }
        } catch (e: Exception) {
            Log.e("AgentVpnService", "Failed to start VPN", e)
        }
    }

    private fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
        Log.d("AgentVpnService", "VPN Stopped - Traffic restored")
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.fyp_hotspot_mobility.agent.STOP_VPN"
    }
}
