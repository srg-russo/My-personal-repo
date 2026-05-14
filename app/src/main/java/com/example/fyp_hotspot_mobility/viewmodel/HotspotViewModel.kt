package com.example.fyp_hotspot_mobility.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp_hotspot_mobility.data.DeviceRepository
import com.example.fyp_hotspot_mobility.data.DeviceScanner
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Responsible for scanning devices, tracking blocked state and bandwidth caps.
 */
class HotspotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceRepository(application)

    private val _uiState = MutableStateFlow(HotspotUiState())
    val uiState: StateFlow<HotspotUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Fetch initial hotspot info on startup without starting a full device scan
        viewModelScope.launch {
            val (ssid, isEnabled) = withContext(Dispatchers.IO) {
                DeviceScanner.getHotspotSsid(getApplication()) to DeviceScanner.isHotspotEnabled(getApplication())
            }
            _uiState.update { it.copy(ssid = ssid, isHotspotEnabled = isEnabled) }
        }
    }

    /**
     * Performs a network scan and refreshes the device list.
     */
    fun scanDevices() {
        if (_uiState.value.isScanning) return

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }

            try {
                // 1. Get raw scan data and hotspot info on the IO thread
                val scanData = withContext(Dispatchers.IO) {
                    val devices = DeviceScanner.scanConnectedDevices(getApplication())
                    val ssid = DeviceScanner.getHotspotSsid(getApplication())
                    val isEnabled = DeviceScanner.isHotspotEnabled(getApplication())
                    Triple(devices, ssid, isEnabled)
                }

                val rawDevices = scanData.first
                val currentSsid = scanData.second
                val currentIsEnabled = scanData.third

                // 2. Get blocked IDs from the repository
                val blockedIds = repository.blockedDeviceIds.value

                // 3. Enrich the device data 
                val enriched = rawDevices.map { device: ConnectedDevice ->
                    val nickname = repository.getNickname(device.id).takeUnless { it.isNullOrBlank() } 
                        ?: device.hostname
                    
                    // Simulate some usage for demo purposes
                    repository.addUsage(device.id, (0..5).random().toFloat())
                    
                    val limit = repository.getDataLimit(device.id)
                    val usage = repository.getUsage(device.id)
                    
                    device.copy(
                        hostname = nickname,
                        isBlocked = blockedIds.contains(device.id),
                        dataLimitMb = limit,
                        usageMb = usage
                    )
                }

                // Check for limits exceeded
                val exceededNames = enriched.filter { it.dataLimitMb != null && it.usageMb > it.dataLimitMb }
                    .joinToString(", ") { it.hostname }
                
                val alert = if (exceededNames.isNotEmpty()) {
                    "Data limit exceeded for: $exceededNames"
                } else null

                // 4. Update the UI state
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        ssid = currentSsid,
                        isHotspotEnabled = currentIsEnabled,
                        devices = enriched,
                        hasScannedAtLeastOnce = true,
                        limitExceededAlert = alert
                    ) 
                }

            } catch (e: CancellationException) {
                // Ignore cancellation
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun blockDevice(id: String) {
        repository.blockDevice(id)
        updateDeviceState(id) { it.copy(isBlocked = true) }
    }

    fun unblockDevice(id: String) {
        repository.unblockDevice(id)
        updateDeviceState(id) { it.copy(isBlocked = false) }
    }

    fun setDataLimit(id: String, limitMb: Int?) {
        repository.setDataLimit(id, limitMb)
        updateDeviceState(id) { it.copy(dataLimitMb = limitMb) }
        
        // Check if this new limit is already exceeded
        val device = _uiState.value.devices.find { it.id == id }
        if (device != null && limitMb != null && device.usageMb > limitMb) {
            _uiState.update { it.copy(limitExceededAlert = "Data limit exceeded for: ${device.hostname}") }
        }
    }

    fun updateDeviceNickname(id: String, name: String) {
        repository.setNickname(id, name)
        updateDeviceState(id) { it.copy(hostname = name) }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(limitExceededAlert = null) }
    }

    private fun updateDeviceState(id: String, transform: (ConnectedDevice) -> ConnectedDevice) {
        _uiState.update { state ->
            state.copy(devices = state.devices.map { if (it.id == id) transform(it) else it })
        }
    }
}
