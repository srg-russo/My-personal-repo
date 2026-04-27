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
                    
                    val randomSpeed = (0..300).random().toFloat()
                    
                    device.copy(
                        hostname = nickname,
                        isBlocked = blockedIds.contains(device.id),
                        bandwidthLimitKbps = repository.getBandwidthLimit(device.id),
                        downloadSpeed = randomSpeed,
                        uploadSpeed = (0..120).random().toFloat()
                    )
                }

                // 4. Update the UI state
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        ssid = currentSsid,
                        isHotspotEnabled = currentIsEnabled,
                        devices = enriched,
                        hasScannedAtLeastOnce = true
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

    fun setBandwidthLimit(id: String, limitKbps: Int?) {
        repository.setBandwidthLimit(id, limitKbps)
        updateDeviceState(id) { it.copy(bandwidthLimitKbps = limitKbps) }
    }

    fun updateDeviceNickname(id: String, name: String) {
        repository.setNickname(id, name)
        updateDeviceState(id) { it.copy(hostname = name) }
    }

    private fun updateDeviceState(id: String, transform: (ConnectedDevice) -> ConnectedDevice) {
        _uiState.update { state ->
            state.copy(devices = state.devices.map { if (it.id == id) transform(it) else it })
        }
    }

    // Unused but kept for structure if needed later
    fun startAutoRefresh(lifecycle: androidx.lifecycle.Lifecycle) {
        // Implementation removed as requested to keep scan manual
    }
}
