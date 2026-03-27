package com.example.fyp_hotspot_mobility.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.example.fyp_hotspot_mobility.data.DeviceRepository
import com.example.fyp_hotspot_mobility.data.DeviceScanner
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Responsible for scanning devices, tracking blocked state and bandwidth caps.
 */

class HotspotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceRepository(application)

    private val _uiState = MutableStateFlow(HotspotUiState())
    val uiState: StateFlow<HotspotUiState> = _uiState.asStateFlow()

    fun startAutoRefresh(lifecycle: Lifecycle) {
        viewModelScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    scanDevices()
                    delay(5_000)
                }
            }
        }
    }

    /**
     * Performs a network scan and refreshes the device list.
     */
    fun scanDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val ssid = DeviceScanner.getHotspotSsid(getApplication())
            val hotspotEnabled = DeviceScanner.isHotspotEnabled(getApplication())

            val rawDevices = try {
                DeviceScanner.scanConnectedDevices(getApplication())
            } catch (_: Exception) {
                emptyList<ConnectedDevice>()
            }

            val blockedIds = repository.blockedDeviceIds.value

            val enriched = rawDevices.map { device ->
                val nickname = repository.getNickname(device.id).takeUnless { it.isNullOrBlank() } ?: device.hostname
                val randomSpeed = (0..300).random().toFloat() // simulated KB/s
                device.copy(
                    hostname = nickname,
                    isBlocked = blockedIds.contains(device.id),
                    bandwidthLimitKbps = repository.getBandwidthLimit(device.id),
                    downloadSpeed = randomSpeed,
                    uploadSpeed = (0..120).random().toFloat()
                )
            }

            _uiState.update {
                it.copy(
                    ssid = ssid,
                    isHotspotEnabled = hotspotEnabled,
                    devices = enriched,
                    isScanning = false
                )
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
