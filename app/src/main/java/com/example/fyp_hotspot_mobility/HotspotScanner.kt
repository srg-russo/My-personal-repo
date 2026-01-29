package com.example.fyp_hotspot_mobility

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

object HotspotScanner {
    data class DeviceInfo(
        val ip: String,
        val mac: String,
        val hostname: String?
    )

    private suspend fun resolveHostnameWithNslookup(ip: String, timeoutMs: Long = 500): String? = withContext(Dispatchers.IO) {
        // First try Java reverse DNS (may return the IP itself)
        try {
            val inet = InetAddress.getByName(ip)
            val host = inet.hostName
            if (!host.isNullOrBlank() && host != ip) return@withContext host
        } catch (_: Exception) { /* ignore */ }

        // Try nslookup if available on device
        try {
            val pb = ProcessBuilder("nslookup", ip)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return@withContext null
            }
            BufferedReader(InputStreamReader(proc.inputStream)).use { br ->
                br.lineSequence().forEach { line ->
                    // Look for patterns like: "name = host.example.com." or "Name: host.example.com"
                    val lower = line.lowercase()
                    val nameEq = Regex("""name\s*=\s*(\S+)""", RegexOption.IGNORE_CASE).find(line)
                    if (nameEq != null) {
                        var candidate = nameEq.groupValues[1].trim().trimEnd('.')
                        if (candidate.isNotEmpty() && candidate != ip) return@withContext candidate
                    }
                    val nameColon = Regex("""name:\s*(\S+)""", RegexOption.IGNORE_CASE).find(line)
                    if (nameColon != null) {
                        var candidate = nameColon.groupValues[1].trim().trimEnd('.')
                        if (candidate.isNotEmpty() && candidate != ip) return@withContext candidate
                    }
                    val ptrMatch = Regex("""\s+ptr\s+(\S+)""", RegexOption.IGNORE_CASE).find(line)
                    if (ptrMatch != null) {
                        var candidate = ptrMatch.groupValues[1].trim().trimEnd('.')
                        if (candidate.isNotEmpty() && candidate != ip) return@withContext candidate
                    }
                }
            }
        } catch (_: Throwable) {
            // nslookup not present or failed
        }
        null
    }

    suspend fun getConnectedDevices(context: Context): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val arpFile = java.io.File("/proc/net/arp")
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
                        // If hostname still null or equal to ip, try nslookup (suspend)
                        if (hostname == null || hostname == ip) {
                            try {
                                val resolved = runCatching { kotlinx.coroutines.runBlocking { resolveHostnameWithNslookup(ip) } }.getOrNull()
                                if (!resolved.isNullOrBlank()) hostname = resolved
                            } catch (_: Exception) { }
                        }
                        devices.add(DeviceInfo(ip = ip, mac = mac, hostname = hostname))
                    }
                }
            }
        } catch (_: Exception) {
            // if reading /proc/net/arp fails, return whatever collected so far
        }

        devices
    }

    // Helper used by MainActivity scan (if MainActivity calls this directly)
    suspend fun resolveHostname(ip: String, timeoutMs: Long = 500): String? = resolveHostnameWithNslookup(ip, timeoutMs)
}
