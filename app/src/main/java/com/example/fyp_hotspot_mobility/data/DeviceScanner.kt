package com.example.fyp_hotspot_mobility.data

import android.content.Context
import android.net.wifi.WifiManager
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withPermit
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Locale

object DeviceScanner {
    private const val TAG = "DeviceScanner"

    private fun getHotspotLocalIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
            val hotspotInterfaceNames = listOf("ap", "wlan1", "softap", "bridge", "rndis", "swlan", "wlan0")
            for (intf in interfaces) {
                val name = intf.name.lowercase(Locale.US)
                if (hotspotInterfaceNames.any { name.contains(it) } && intf.isUp && !intf.isLoopback) {
                    val addrs = intf.inetAddresses ?: continue
                    for (addr in addrs) {
                        val host = addr.hostAddress
                        if (host != null && host.contains(".") && !addr.isLinkLocalAddress) {
                            if (host.startsWith("192.168.43.") || host.startsWith("172.20.")) return host
                        }
                    }
                    for (addr in intf.inetAddresses) {
                        val host = addr.hostAddress
                        if (host != null && host.contains(".") && !addr.isLinkLocalAddress) return host
                    }
                }
            }

            for (intf in interfaces) {
                val name = intf.name.lowercase(Locale.US)
                if (name.contains("rmnet") || name.contains("pdp") || name.contains("ccmni") || 
                    name.contains("vzw") || name.contains("p2p") || !intf.isUp || intf.isLoopback) continue
                
                val addrs = intf.inetAddresses ?: continue
                for (addr in addrs) {
                    val host = addr.hostAddress
                    if (host != null && host.contains(".") && !addr.isLinkLocalAddress) {
                        if (host.startsWith("192.168.") || host.startsWith("172.") || host.startsWith("10.")) return host
                    }
                }
            }
        } catch (e: Exception) { }
        return null
    }

    suspend fun scanConnectedDevices(
        onDeviceFound: (ConnectedDevice) -> Unit
    ) = coroutineScope {
        val localIp = getHotspotLocalIp() ?: "192.168.43.1"
        val subnetPrefix = localIp.substringBeforeLast(".") + "."
        
        android.util.Log.d(TAG, "Scanning using localIp: $localIp, Subnet: $subnetPrefix*")

        // Scan the detected subnet for any devices
        val scanSemaphore = kotlinx.coroutines.sync.Semaphore(100) 
        val scanJobs = (1..254).map { i ->
            launch(Dispatchers.IO) {
                scanSemaphore.withPermit {
                    val ip = subnetPrefix + i
                    if (ip == localIp) return@launch

                    try {
                        val address = InetAddress.getByName(ip)
                        try {
                            DatagramSocket().use { udpSocket ->
                                val buf = ByteArray(1)
                                val packet = DatagramPacket(buf, buf.size, address, 49152)
                                udpSocket.send(packet)
                            }
                        } catch (_: Exception) { }

                        var isReachable = try { address.isReachable(350) } catch(_: Exception) { false }
                        
                        if (!isReachable) {
                            // Probe other non-HTTP ports to detect active devices
                            isReachable = isPortOpen(ip, 137, 150)
                        }

                        if (isReachable) {
                            val device = ConnectedDevice(
                                id = ip, // Using IP as identifier since MAC is inaccessible, fallback for devices that do not have the companion app installed
                                ipAddress = ip,
                                hostname = "Device at .$i",
                                isBlocked = false,
                                dataLimitMb = null,
                                usageMb = 0f
                            )
                            onDeviceFound(device)
                        }
                    } catch (_: Exception) { }
                }
            }
        }
        
        scanJobs.joinAll()
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (e: Exception) { false }
    }

    fun isHotspotEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return try {
            val method = wifiManager?.javaClass?.getMethod("isWifiApEnabled")
            method?.invoke(wifiManager) as? Boolean ?: false
        } catch (e: Exception) { false }
    }
}
