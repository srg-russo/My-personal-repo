package com.example.fyp_hotspot_mobility.data

import android.content.Context
import android.net.wifi.WifiManager
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Locale

/**
 * Helper for discovering devices connected to the local hotspot.
 *
 * NOTE: On modern Android versions, getting accurate hotspot state and SSID is restricted.
 * This implementation uses the best-effort reflection approach for Wi-Fi AP state.
 */
object DeviceScanner {
    private val arpLineRegex = Regex("^(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+\\d+\\s+\\d+\\s+([0-9a-fA-F:]{17})\\s+.*")

    /**
     * Parse /proc/net/arp and return pairs of (ip, mac).
     */
    private fun parseArp(): List<Pair<String, String>> {
        val file = File("/proc/net/arp")
        if (!file.exists() || !file.canRead()) return emptyList()

        return file.readLines().drop(1) // skip header
            .mapNotNull { line ->
                arpLineRegex.find(line)?.let { match ->
                    val ip = match.groupValues[1]
                    val mac = match.groupValues[2].lowercase(Locale.US)
                    // Ignore invalid MAC entries / placeholders
                    if (mac == "00:00:00:00:00:00") null else ip to mac
                }
            }
    }

    /**
     * Scans connected devices and returns a list of [ConnectedDevice].
     * This is a best-effort scan; results depend on platform restrictions.
     */
    suspend fun scanConnectedDevices(
        context: Context,
        timeoutMs: Int = 5000): List<ConnectedDevice> {
        return withContext(Dispatchers.IO) {
            
            val arpDevices = parseArp()
            
            val devicesToScan = if (arpDevices.isNotEmpty()) {
                arpDevices
            } else {
            scanSubnet()
            }

            devicesToScan.mapNotNull { (ip, mac) ->

                try {
                    val address = InetAddress.getByName(ip)

                    val reachable = try {
                        val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 1 $ip")
                        process.waitFor() == 0
                    } catch (_: Exception) {
                        false
                    }

                    if (!reachable) return@mapNotNull null

                    val hostname = try {
                        val name = address.hostName
                        if (name.isNullOrBlank() || name == ip) "Unknown Device" else name
                    } catch (e: Exception) {
                        "Unknown Device"
                    }
            
                    ConnectedDevice(
                        id = mac,
                        ipAddress = ip,
                        hostname = hostname,
                        isBlocked = false,
                        downloadSpeed = 0f,
                        uploadSpeed = 0f,
                        bandwidthLimitKbps = null
                    )
                } catch (_: Exception) {
                    null
                
                }
            }
        }
    }

    // fallback scan function
    Private fun scanSubnet(): List<Pair<String, String>> {
        val devices = mutableListOf<Pair<String, String>>()
        for (i in 1..254){
            val ip = "192.168.43.$i"

            val reachable = try {
                val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 1 $ip")
                process.waitFor() == 0 
            } catch (e: Exception) {
                false
            }
            if (reachable) {
                devices.add(ip to "unknown-mac")
            }
        }
        return devices 
    }

    // Try to obtain the local IP address of a connected device if proc/net/arp fails  
    fun getlocalip(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            for (addr in intf.inetAddresses) {
                if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

    /**
     * Try to obtain the current hotspot SSID.
     */
    fun getHotspotSsid(context: Context): String? {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val method = wifiManager?.javaClass?.getMethod("getWifiApConfiguration")
        val config = method?.invoke(wifiManager)
        
        // Use safe call and let to handle nullability
        config?.toString()?.let { configString ->
            val regex = Regex("ssid=(.*?)(?:,|\$)", RegexOption.IGNORE_CASE)
            regex.find(configString)?.groupValues?.getOrNull(1)
        }
    } catch (e: Exception) {
        e.printStackTrace() // Log the error for debugging
        null
    }
}

// checking if hotspot is enabled

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