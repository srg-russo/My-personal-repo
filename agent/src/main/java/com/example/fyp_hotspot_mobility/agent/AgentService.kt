package com.example.fyp_hotspot_mobility.agent

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class AgentService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "AgentServiceChannel"
    private val NOTIFICATION_ID = 1

    companion object {
        fun getNetworkRequest(): NetworkRequest {
            return NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build()
        }

        // This is the "Trap" we set in the Android OS
        fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AgentService::class.java)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Searching for Hostwatch..."))
        startHeartbeat()
        startCommandListener()
    }

    private var heartbeatJob: Job? = null
    private var isBlockedByHost: Boolean
        get() = getSharedPreferences("agent_prefs", Context.MODE_PRIVATE).getBoolean("is_blocked", false)
        set(value) = getSharedPreferences("agent_prefs", Context.MODE_PRIVATE).edit().putBoolean("is_blocked", value).apply()

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        
        heartbeatJob = serviceScope.launch {
            var failureCount = 0
            while (isActive) {
                val gatewayIp = getGatewayIp()
                if (gatewayIp != null) {
                    val success = sendHandshake(gatewayIp)
                    if (success) {
                        failureCount = 0
                        updateNotification(if (isBlockedByHost) "Internet restricted by Host" else "Connected to Hostwatch Host")
                        
                        if (isBlockedByHost) startVpn() else stopVpn()
                    } else {
                        Log.w("AgentService", "Handshake failed to $gatewayIp")
                        failureCount++
                        stopVpn()
                    }
                } else {
                    Log.d("AgentService", "No Gateway IP detected yet")
                    failureCount++
                    stopVpn()
                }

                // If no Hostwatch found for ~2 minutes, stop service
                if (failureCount >= 12) {
                    Log.d("AgentService", "Hostwatch not found. Stopping service to save battery.")
                    stopVpn()
                    stopSelf()
                    break
                }
                delay(10000)
            }
        }
    }

    private fun getGatewayIp(): String? {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wm.dhcpInfo
        
        // 1. Try WifiManager DHCP Info (most reliable for Hotspot gateway)
        if (dhcp != null && dhcp.gateway != 0) {
            val gateway = Formatter.formatIpAddress(dhcp.gateway)
            if (gateway != "0.0.0.0") {
                Log.d("AgentService", "Gateway from DHCP: $gateway")
                return gateway
            }
        }

        // 2. Fallback to ConnectivityManager LinkProperties
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiNetwork = cm.allNetworks.firstOrNull { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        
        if (wifiNetwork != null) {
            val linkProps = cm.getLinkProperties(wifiNetwork)
            val gatewayRoute = linkProps?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
            if (gatewayRoute != null) {
                Log.d("AgentService", "Gateway from Routes: $gatewayRoute")
                return gatewayRoute
            }
            
            // 3. Fallback to Subnet .1 logic
            val ipv4Addr = linkProps?.linkAddresses?.firstOrNull { 
                val addr = it.address.hostAddress
                addr != null && addr.contains(".") 
            }?.address?.hostAddress
            
            if (ipv4Addr != null) {
                val gateway = ipv4Addr.substringBeforeLast(".") + ".1"
                Log.d("AgentService", "Gateway from Subnet Fallback: $gateway")
                return gateway
            }
        }
        
        return null
    }

    private suspend fun sendHandshake(hostIp: String): Boolean = withContext(Dispatchers.IO) {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiNetwork = cm.allNetworks.firstOrNull { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        try {
            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            val uniqueId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

            //Track data on This device from connection start
            val rxBytes = TrafficStats.getTotalRxBytes()
            val txBytes = TrafficStats.getTotalTxBytes()
            val totalBytes = rxBytes + txBytes

            val message = "$deviceName|$uniqueId|$totalBytes"
            val data = message.toByteArray()
            
            DatagramSocket().use { socket ->
                try { wifiNetwork?.bindSocket(socket) } catch(e: Exception) { Log.w("AgentService", "Could not bind to WiFi network") }
                
                val address = InetAddress.getByName(hostIp)
                val packet = DatagramPacket(data, data.size, address, 8888)
                socket.send(packet)
                
                Log.d("AgentService", "Handshake sent to $hostIp")
                true
            }
        } catch (e: Exception) {
            Log.e("AgentService", "Handshake error", e)
            false
        }
    }

    private fun startVpn() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val hasWifi = cm.allNetworks.any { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        if (hasWifi) {
            val intent = Intent(this, AgentVpnService::class.java)
            startService(intent)
        } else {
            stopVpn()
        }
    }

    private fun stopVpn() {
        val intent = Intent(this, AgentVpnService::class.java).apply {
            action = AgentVpnService.ACTION_STOP
        }
        startService(intent)
    }

    private fun startCommandListener() {
        serviceScope.launch {
            var socket: DatagramSocket? = null
            try {
                // Try to bind specifically to the WiFi network to receive commands even when VPN is active
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val wifiNetwork = cm.allNetworks.firstOrNull { network ->
                    cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                }

                socket = DatagramSocket(8889)
                try { wifiNetwork?.bindSocket(socket) } catch (e: Exception) { Log.w("AgentService", "Could not bind command socket to WiFi") }
                
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    withContext(Dispatchers.IO) { 
                        socket.soTimeout = 5000
                        try {
                            socket.receive(packet) 
                        } catch (e: java.net.SocketTimeoutException) {
                            // Loop around
                        }
                    }
                    
                    if (packet.length == 0) continue
                    
                    val command = String(packet.data, 0, packet.length).trim()
                    Log.d("AgentService", "Received command: $command")

                    when (command) {
                        "BLOCK" -> {
                            isBlockedByHost = true
                            startVpn()
                            updateNotification("Internet restricted by Host")
                        }
                        "UNBLOCK" -> {
                            isBlockedByHost = false
                            stopVpn()
                            updateNotification("Connected to Hostwatch Host")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AgentService", "Command listener error", e)
            } finally {
                socket?.close()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Hostwatch Agent Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hostwatch Agent")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }
}
