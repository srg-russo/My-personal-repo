package com.example.fyp_hotspot_mobility.viewmodel

// import package com.example.fyp_hotspot_mobility.data.DeviceScanner
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp_hotspot_mobility.data.DeviceRepository
import com.example.fyp_hotspot_mobility.data.DeviceScanner
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Responsible for scanning devices, tracking blocked state and bandwidth caps.
 */

class HotspotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceRepository(application)

    private val _uiState = MutableStateFlow(HotspotUiState())
    val uiState: StateFlow<HotspotUiState> = _uiState.asStateFlow()

    fun startAutoRefresh() {
        viewModelScope.launch {
                while (isActive) {
                    scanDevices()
                    delay(5_000)
            }
        }
    }

    /**
     * Performs a network scan and refreshes the device list.
     */
    fun scanDevices() {
    viewModelScope.launch {
        _uiState.update { it.copy(isScanning = true) }

        try {
            //  Get the raw data on the IO thread
            val rawDevices = withContext(Dispatchers.IO) {
                DeviceScanner.scanConnectedDevices(getApplication())
            }

            // 2. Get blocked IDs from the repository
            val blockedIds = repository.blockedDeviceIds.value

            //  Enrich the data 
            val enriched = rawDevices.map { device ->
                val nickname = repository.getNickname(device.id).takeUnless { it.isNullOrBlank() } 
                    ?: device.hostname
                
                val randomSpeed = (0..300).random().toFloat()
                
                // Use the 'device' object here while it's in scope
                device.copy(
                    hostname = nickname,
                    isBlocked = blockedIds.contains(device.id),
                    bandwidthLimitKbps = repository.getBandwidthLimit(device.id),
                    downloadSpeed = randomSpeed,
                    uploadSpeed = (0..120).random().toFloat()
                )
            }

            // Update the UI state with the enriched list
            _uiState.update { 
                it.copy(
                    isScanning = false,
                    devices = enriched 
                ) 
            }

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
}
