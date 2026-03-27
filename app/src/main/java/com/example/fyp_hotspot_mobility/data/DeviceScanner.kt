package com.example.fyp_hotspot_mobility.data

import android.content.Context
import android.net.wifi.WifiManager
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
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

        return file.readLines()
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
    suspend fun scanConnectedDevices(context: Context, timeoutMs: Int = 250): List<ConnectedDevice> {
        return withContext(Dispatchers.IO) {
            parseArp().mapNotNull { (ip, mac) ->
                try {
                    val address = InetAddress.getByName(ip)
                    val reachable = address.isReachable(timeoutMs)
                    if (!reachable) return@mapNotNull null

                    val hostname = try {
                        val name = address.canonicalHostName
                        if (name.isNullOrBlank() || name == ip) "Unknown Device" else name
                    } catch (_: Exception) {
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

    /**
     * Try to obtain the current hotspot SSID.
     */
    fun getHotspotSsid(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val method = wifiManager?.javaClass?.getMethod("getWifiApConfiguration")
            val config = method?.invoke(wifiManager)?.toString()
            config?.let {
                // This will contain SSID=... in toString for older APIs
                Regex("ssid=(.*?)(,|$)").find(it)?.groupValues?.get(1)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Best-effort hotspot enabled check.
     */
    fun isHotspotEnabled(context: Context): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val method = wifiManager?.javaClass?.getMethod("isWifiApEnabled")
            (method?.invoke(wifiManager) as? Boolean) == true
        } catch (_: Exception) {
            false
        }
    }
}
