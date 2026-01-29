package com.example.fyp_hotspot_mobility

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress

object HotspotScanner {
    data class DeviceInfo(
        val ip: String,
        val mac: String,
        val hostname: String?
    )

    suspend fun getConnectedDevices(context: Context): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val arpFile = File("/proc/net/arp")
        if (!arpFile.exists()) return@withContext emptyList()

        val devices = mutableListOf<DeviceInfo>()
        try {
            arpFile.forEachLine { line ->
                if (line.startsWith("IP")) return@forEachLine
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 6) {
                    val ip = parts[0]
                    val mac = parts[3]
                    if (mac != "00:00:00:00:00:00") {
                        var hostname: String? = null
                        try {
                            val inet = InetAddress.getByName(ip)
                            val host = inet.hostName
                            if (host != null && host != ip) hostname = host
                        } catch (_: Exception) {
                        }
                        devices.add(DeviceInfo(ip = ip, mac = mac, hostname = hostname))
                    }
                }
            }
        } catch (e: Exception) {
            // if reading /proc/net/arp fails, return whatever collected so far
        }

        devices
    }
}
