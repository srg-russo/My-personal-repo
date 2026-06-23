package com.example.fyp_hotspot_mobility.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp_hotspot_mobility.data.*
import com.example.fyp_hotspot_mobility.data.local.AppDatabase
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Scanning devices, tracking blocked state and bandwidth limits, the brain of the entire application .
class HotspotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val dnsProxy = DnsProxyServer(repository)

    private val _uiState = MutableStateFlow(HotspotUiState())
    val uiState: StateFlow<HotspotUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var trackingJob: Job? = null
    private val dismissedExceededDeviceIds = mutableSetOf<String>()
    private val agentCache = mutableMapOf<String, Pair<String, String>>() // IP -> Pair(Name, ID)

    init {
        // Fetch initial hotspot info on startup
        viewModelScope.launch {
            val (ssid, isEnabled) = withContext(Dispatchers.IO) {
                DeviceScanner.getHotspotSsid(getApplication()) to DeviceScanner.isHotspotEnabled(getApplication())
            }
            _uiState.update { it.copy(ssid = ssid, isHotspotEnabled = isEnabled) }
        }
        
        startBandwidthTracking()
        startAgentListener() // Listen for Companion App check-ins
    }

    private fun startAgentListener() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                var socket: java.net.DatagramSocket? = null
                try {
                    socket = java.net.DatagramSocket(8888)
                    socket.reuseAddress = true // Allow immediate restart
                    val buffer = ByteArray(1024)
                    
                    while (isActive) {
                        val packet = java.net.DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val data = String(packet.data, 0, packet.length)
                        val clientIp = packet.address.hostAddress ?: continue
                        
                        val parts = data.split("|")
                        if (parts.size >= 2) {
                            val deviceName = parts[0]
                            val uniqueId = parts[1]
                            val clientReportedTotalBytes = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                            
                            repository.setNickname(uniqueId, deviceName)
                            repository.saveIpMapping(clientIp, uniqueId)
                            agentCache[clientIp] = deviceName to uniqueId

                            if (clientReportedTotalBytes > 0) {
                                updateUsageFromAgent(uniqueId, clientReportedTotalBytes)
                            }

                            withContext(Dispatchers.Main) {
                                refreshUiWithAgentData(clientIp, deviceName, uniqueId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HotspotViewModel", "Agent Listener error, restarting...", e)
                    delay(2000)
                } finally {
                    socket?.close()
                }
            }
        }
    }

    private val lastAgentByteCounts = mutableMapOf<String, Long>() // DeviceId -> TotalBytes

    private fun updateUsageFromAgent(deviceId: String, currentTotalBytes: Long) {
        val lastBytes = lastAgentByteCounts[deviceId]
        if (lastBytes != null && currentTotalBytes > lastBytes) {
            val deltaBytes = currentTotalBytes - lastBytes
            val deltaMb = deltaBytes / (1024f * 1024f)
            
            // Only add if delta is reasonable (e.g. less than 100MB per 10s)
            if (deltaMb < 100f) {
                repository.addUsage(deviceId, deltaMb)
            }
        }
        lastAgentByteCounts[deviceId] = currentTotalBytes
    }

    private fun refreshUiWithAgentData(ip: String, name: String, id: String) {
        _uiState.update { state ->
            val updatedList = state.devices.map { 
                if (it.ipAddress == ip) {
                    it.copy(hostname = name, id = id) 
                } else it
            }
            state.copy(devices = updatedList)
        }
    }

    private fun startBandwidthTracking() {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val isEnabled = DeviceScanner.isHotspotEnabled(getApplication())
                val ssid = if (isEnabled) DeviceScanner.getHotspotSsid(getApplication()) else null
                
                withContext(Dispatchers.Main) {
                    if (_uiState.value.isHotspotEnabled != isEnabled || _uiState.value.ssid != ssid) {
                        _uiState.update { it.copy(isHotspotEnabled = isEnabled, ssid = ssid) }
                    }
                    
                    // Periodically refresh usage from repository (updated by agents)
                    refreshUsageAndCheckLimits()
                }

                if (isEnabled) {
                    dnsProxy.start()
                } else {
                    dnsProxy.stop()
                }
                delay(3000) // Track every 3 seconds
            }
        }
    }

    private fun refreshUsageAndCheckLimits() {
        val updatedDevices = _uiState.value.devices.map { device ->
            device.copy(usageMb = repository.getUsage(device.id))
        }
        
        // Find devices that are over the limit and NOT already blocked
        val newlyExceededDevices = updatedDevices.filter { 
            it.dataLimitMb != null && it.usageMb > it.dataLimitMb && !it.isBlocked
        }

        // Automatically block devices that just exceeded their limit
        newlyExceededDevices.forEach { device ->
            android.util.Log.d("HotspotViewModel", "Auto-blocking ${device.hostname} - Exceeded limit (${device.usageMb}/${device.dataLimitMb} MB)")
            blockDevice(device.id)
        }

        // Update list again to reflect the new blocked states
        val finalDevices = _uiState.value.devices.map { device ->
            device.copy(
                usageMb = repository.getUsage(device.id),
                isBlocked = repository.isBlocked(device.id)
            )
        }

        // Alert logic
        val overLimitIds = updatedDevices.filter { it.dataLimitMb != null && it.usageMb > it.dataLimitMb }.map { it.id }.toSet()
        dismissedExceededDeviceIds.retainAll(overLimitIds)

        val newlyExceededNames = newlyExceededDevices
            .filter { !dismissedExceededDeviceIds.contains(it.id) }
            .joinToString(", ") { it.hostname }
            
        val alert = if (newlyExceededNames.isNotEmpty()) {
            "Data limit exceeded and device(s) blocked: $newlyExceededNames"
        } else {
            _uiState.value.limitExceededAlert
        }

        _uiState.update { 
            it.copy(
                devices = finalDevices,
                limitExceededAlert = alert
            )
        }
    }

    // Performs a network scan and refreshes the device list.
    fun scanDevices() {
        if (_uiState.value.isScanning) return

        scanJob?.cancel()

        _uiState.update { it.copy(isScanning = true) }

        // Wi-Fi Scan (Main UI Task)
        scanJob = viewModelScope.launch {
            try {
                // Update basic info first
                val (ssid, isEnabled) = withContext(Dispatchers.IO) {
                    DeviceScanner.getHotspotSsid(getApplication()) to DeviceScanner.isHotspotEnabled(getApplication())
                }
                _uiState.update { it.copy(ssid = ssid, isHotspotEnabled = isEnabled, devices = emptyList()) }

                val blockedIds = repository.blockedDeviceIds.value


                // Perform the scan (this now returns as soon as the IP sweep is done)
                DeviceScanner.scanConnectedDevices { rawDevice ->
                    viewModelScope.launch {
                        // 1. Try to find the identity of this IP
                        val cachedInfo = agentCache[rawDevice.ipAddress]
                        val persistentId = repository.getUniqueIdForIp(rawDevice.ipAddress)
                        
                        val effectiveId = cachedInfo?.second ?: persistentId ?: rawDevice.id
                        
                        // 2. Look up the saved name for this identity
                        val savedName = repository.getNickname(effectiveId)
                        val nickname = repository.getNickname(rawDevice.id).takeUnless { it.isNullOrBlank() } 
                        
                        val hostname = rawDevice.hostname

                        val enriched = rawDevice.copy(
                            hostname = savedName ?: nickname ?: hostname,
                            id = effectiveId,
                            isBlocked = repository.isBlocked(effectiveId),
                            dataLimitMb = repository.getDataLimit(effectiveId),
                            usageMb = repository.getUsage(effectiveId)
                        )

                        // Update the list immediately
                        _uiState.update { state ->
                            val currentList = state.devices.toMutableList()
                            val existingIndex = currentList.indexOfFirst { it.id == enriched.id || it.ipAddress == enriched.ipAddress }
                            
                            if (existingIndex != -1) {
                                // Only update if we have a better name (not a default placeholder)
                                val existing = currentList[existingIndex]
                                val betterName = if (existing.hostname.startsWith("Device") && !enriched.hostname.startsWith("Device")) {
                                    enriched.hostname
                                } else {
                                    existing.hostname
                                }
                                currentList[existingIndex] = enriched.copy(hostname = betterName)
                            } else {
                                currentList.add(enriched)
                            }
                            state.copy(devices = currentList.sortedBy { it.ipAddress })
                        }

                    }
                }

                // IP scan is done! Stop the loading indicator.
                _uiState.update { 
                    it.copy(
                        isScanning = false, 
                        hasScannedAtLeastOnce = true
                    ) 
                }
                
                refreshUsageAndCheckLimits()

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _uiState.update { it.copy(isScanning = false) }
                }
            }
        }
    }

    fun blockDevice(id: String) {
        repository.blockDevice(id)
        updateDeviceState(id) { it.copy(isBlocked = true) }
        sendCommandToAgent(id, "BLOCK")
    }

    fun unblockDevice(id: String) {
        repository.unblockDevice(id)
        
        // If the device was blocked because it exceeded its limit, remove the limit
        val device = _uiState.value.devices.find { it.id == id }
        if (device != null && device.dataLimitMb != null && device.usageMb > device.dataLimitMb) {
            android.util.Log.d("HotspotViewModel", "Removing limit for $id upon manual unblock")
            repository.setDataLimit(id, null)
            updateDeviceState(id) { it.copy(isBlocked = false, dataLimitMb = null) }
        } else {
            updateDeviceState(id) { it.copy(isBlocked = false) }
        }

        sendCommandToAgent(id, "UNBLOCK")
    }

    private fun sendCommandToAgent(deviceId: String, command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Try to find IP from cache (most reliable for Agent devices)
            var ip = agentCache.filterValues { it.second == deviceId }.keys.firstOrNull()
            
            // 2. Fallback to the IP in the UI state
            if (ip == null) {
                ip = _uiState.value.devices.find { it.id == deviceId }?.ipAddress
            }
            
            android.util.Log.d("HotspotViewModel", "Attempting to send $command to $deviceId at IP: $ip")
            
            if (ip != null) {
                try {
                    java.net.DatagramSocket().use { socket ->
                        val data = command.toByteArray()
                        val address = java.net.InetAddress.getByName(ip)
                        val packet = java.net.DatagramPacket(data, data.size, address, 8889)
                        
                        // Send 10 times to account for UDP packet loss and potential VPN interference
                        repeat(10) {
                            socket.send(packet)
                            delay(150)
                        }
                        android.util.Log.d("HotspotViewModel", "Sent $command to Agent at $ip")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HotspotViewModel", "Failed to send command to Agent", e)
                }
            } else {
                android.util.Log.e("HotspotViewModel", "Cannot send command: No IP found for $deviceId")
            }
        }
    }

    fun setDataLimit(id: String, limitMb: Int?) {
        repository.setDataLimit(id, limitMb)
        updateDeviceState(id) { it.copy(dataLimitMb = limitMb) }
    }

    fun updateDeviceNickname(id: String, name: String) {
        repository.setNickname(id, name)
        updateDeviceState(id) { it.copy(hostname = name) }
    }

    fun dismissAlert() {
        // Mark all currently exceeded devices as dismissed
        val currentlyExceededIds = _uiState.value.devices
            .filter { it.dataLimitMb != null && it.usageMb > it.dataLimitMb }
            .map { it.id }
        
        dismissedExceededDeviceIds.addAll(currentlyExceededIds)

        _uiState.update { it.copy(limitExceededAlert = null) }
    }

    private fun updateDeviceState(id: String, transform: (ConnectedDevice) -> ConnectedDevice) {
        _uiState.update { state ->
            state.copy(devices = state.devices.map { if (it.id == id) transform(it) else it })
        }
    }

    override fun onCleared() {
        super.onCleared()
        dnsProxy.stop()
    }
}
