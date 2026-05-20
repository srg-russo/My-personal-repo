package com.example.fyp_hotspot_mobility.data

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Locale

/**
 * Helper for discovering devices connected to the local hotspot.
 */
object DeviceScanner {
    private val arpLineRegex = Regex("^(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+\\d+\\s+\\d+\\s+([0-9a-fA-F:]{17})\\s+.*")

    /**
     * Tries to read ARP table. Note: Restricted on Android 10+.
     */
    private fun parseArp(): List<Pair<String, String>> {
        val file = File("/proc/net/arp")
        if (!file.exists() || !file.canRead()) return emptyList()

        return try {
            file.readLines().drop(1).mapNotNull { line ->
                val match = arpLineRegex.find(line)
                if (match != null) {
                    val ip = match.groupValues[1]
                    val mac = match.groupValues[2].lowercase(Locale.US)
                    if (mac == "00:00:00:00:00:00") null else Pair(ip, mac)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Determines the local IP address of the device on the Hotspot interface.
     */
    private fun getHotspotLocalIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            // Prioritize common hotspot interface names
            val hotspotInterfaces = interfaces.filter { 
                it.name.contains("ap", true) || 
                it.name.contains("wlan1", true) || 
                it.name.contains("softap", true) ||
                it.name.contains("bridge", true)
            }
            
            val sortedInterfaces = hotspotInterfaces + (interfaces - hotspotInterfaces.toSet())

            for (intf in sortedInterfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                
                for (addr in intf.inetAddresses) {
                    val host = addr.hostAddress
                    if (host != null && host.contains(".") && !addr.isLinkLocalAddress) {
                        // Common hotspot subnets
                        if (host.startsWith("192.168.") || 
                            host.startsWith("172.") || 
                            host.startsWith("10.")) return host
                    }
                }
            }
        } catch (e: Exception) { }
        return null
    }

    /**
     * Resolves hostname using multiple methods including DNS and system commands.
     */
    private fun resolveHostname(ip: String): String? {
        // Method 1: Try system 'nslookup' command (simulating user request)
        try {
            val process = Runtime.getRuntime().exec("nslookup $ip")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // nslookup output usually contains "name = hostname"
                if (line?.contains("name =", ignoreCase = true) == true) {
                    return line?.substringAfter("name =")?.trim()?.removeSuffix(".")
                }
            }
        } catch (e: Exception) { }

        // Method 2: Java's built-in reverse DNS lookup
        try {
            val address = InetAddress.getByName(ip)
            val name = address.canonicalHostName
            if (!name.isNullOrBlank() && name != ip) return name
            
            val hostName = address.hostName
            if (!hostName.isNullOrBlank() && hostName != ip) return hostName
        } catch (e: Exception) { }

        return null
    }

    suspend fun scanConnectedDevices(
        context: Context
    ): List<ConnectedDevice> = withContext(Dispatchers.IO) {
        val localIp = getHotspotLocalIp() ?: "192.168.43.1"
        val subnetPrefix = localIp.substringBeforeLast(".") + "."
        
        val arpMap = parseArp().toMap()
        
        // Parallel scan for reachable IPs
        (1..254).map { i ->
            async {
                val ip = subnetPrefix + i
                if (ip == localIp) return@async null

                try {
                    val address = InetAddress.getByName(ip)
                    
                    var isReachable = try {
                        val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 1 $ip")
                        process.waitFor() == 0
                    } catch (_: Exception) {
                        false
                    }

                    if (!isReachable) {
                        isReachable = address.isReachable(500) // Increased timeout
                    }

                    // Scan common ports if ping fails. 
                    // Added 62078 (iPhone), 8008/8009 (Google Cast), 5000 (Synology/others)
                    if (!isReachable) {
                        val ports = intArrayOf(135, 139, 445, 80, 443, 62078, 8008, 8009, 5000)
                        for (port in ports) {
                            if (isPortOpen(ip, port, 150)) {
                                isReachable = true
                                break
                            }
                        }
                    }

                    if (isReachable) {
                        val mac = arpMap[ip] ?: "unknown-mac"
                        val hostname = resolveHostname(ip) ?: "Device at .$i"

                        ConnectedDevice(
                            id = if (mac == "unknown-mac") ip else mac,
                            ipAddress = ip,
                            hostname = hostname,
                            isBlocked = false,
                            dataLimitMb = null,
                            usageMb = 0f
                        )
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getHotspotSsid(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val method = wifiManager.javaClass.getMethod("getSoftApConfiguration")
                val config = method.invoke(wifiManager)
                val ssidMethod = config?.javaClass?.getMethod("getSsid")
                (ssidMethod?.invoke(config) as? String)?.trim('\"')
            } else {
                val method = wifiManager.javaClass.getMethod("getWifiApConfiguration")
                val config = method.invoke(wifiManager)
                config?.toString()?.let { configString ->
                    val regex = Regex("ssid=(.*?)(?:,|\$)", RegexOption.IGNORE_CASE)
                    regex.find(configString)?.groupValues?.getOrNull(1)?.trim('\"')
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isHotspotEnabled(context: Context): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val method = wifiManager?.javaClass?.getMethod("isWifiApEnabled")
            method?.invoke(wifiManager) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
}
