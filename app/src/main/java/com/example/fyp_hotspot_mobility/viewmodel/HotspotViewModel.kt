package com.example.fyp_hotspot_mobility.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fyp_hotspot_mobility.data.BluetoothScanner
import com.example.fyp_hotspot_mobility.data.DeviceRepository
import com.example.fyp_hotspot_mobility.data.DeviceScanner
import com.example.fyp_hotspot_mobility.data.local.AppDatabase
import com.example.fyp_hotspot_mobility.data.local.entity.BluetoothProfileEntity
import com.example.fyp_hotspot_mobility.data.local.entity.DiscoveryLogEntity
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import com.example.fyp_hotspot_mobility.pruner.LogPruningWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

// Scanning devices, tracking blocked state and bandwidth limits.
class HotspotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val bluetoothScanner = BluetoothScanner(application)
    private val bluetoothDao = database.bluetoothDao()

    private val _uiState = MutableStateFlow(HotspotUiState())
    val uiState: StateFlow<HotspotUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var bluetoothScanJob: Job? = null

    init {
        // Schedule log pruning
        scheduleLogPruning()

        // Fetch initial hotspot info on startup without starting a full device scan
        viewModelScope.launch {
            val (ssid, isEnabled) = withContext(Dispatchers.IO) {
                DeviceScanner.getHotspotSsid(getApplication()) to DeviceScanner.isHotspotEnabled(getApplication())
            }
            _uiState.update { it.copy(ssid = ssid, isHotspotEnabled = isEnabled) }
        }
    }

    private fun scheduleLogPruning() {
        val pruneRequest = PeriodicWorkRequestBuilder<LogPruningWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "LogPruning",
            ExistingPeriodicWorkPolicy.KEEP,
            pruneRequest
        )
    }

    // Performs a network scan and refreshes the device list.
    fun scanDevices() {
        if (_uiState.value.isScanning) return

        scanJob?.cancel()
        bluetoothScanJob?.cancel()

        _uiState.update { it.copy(isScanning = true, nearbyBluetoothDevices = emptyList()) }

        // Start Bluetooth Scanning in parallel
        bluetoothScanJob = viewModelScope.launch {
            try {
                bluetoothScanner.scanDevices().collect { device ->
                    handleBluetoothDiscovery(device)
                }
            } catch (e: Exception) {
                // Log the error and allow network scan to continue
                android.util.Log.e("HotspotViewModel", "Bluetooth scan failed", e)
            }
        }

        scanJob = viewModelScope.launch {
            try {
                val scanData = withContext(Dispatchers.IO) {
                    val devices = DeviceScanner.scanConnectedDevices(getApplication())
                    val ssid = DeviceScanner.getHotspotSsid(getApplication())
                    val isEnabled = DeviceScanner.isHotspotEnabled(getApplication())
                    Triple(devices, ssid, isEnabled)
                }

                val rawDevices = scanData.first
                val currentSsid = scanData.second
                val currentIsEnabled = scanData.third

                val blockedIds = repository.blockedDeviceIds.value

                val enriched = rawDevices.map { device: ConnectedDevice ->
                    val nickname = repository.getNickname(device.id).takeUnless { it.isNullOrBlank() } 
                        ?: device.hostname
                    
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

                val exceededNames = enriched.filter { it.dataLimitMb != null && it.usageMb > it.dataLimitMb }
                    .joinToString(", ") { it.hostname }
                
                val alert = if (exceededNames.isNotEmpty()) {
                    "Data limit exceeded for: $exceededNames"
                } else null

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

                // Stop Bluetooth scan after network scan finishes (or keep it running for a bit)
                delay(5000)
                bluetoothScanJob?.cancel()

            } catch (e: CancellationException) {
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    private suspend fun handleBluetoothDiscovery(device: BluetoothScanner.DiscoveredBluetoothDevice) {
        withContext(Dispatchers.IO) {
            // 1. Log the discovery
            bluetoothDao.insertDiscoveryLog(
                DiscoveryLogEntity(
                    deviceAddress = device.address,
                    timestamp = device.timestamp,
                    rssi = device.rssi,
                    isHotspotActive = _uiState.value.isHotspotEnabled,
                    totalTrafficAtTime = 0 // Would integrate with TrafficStats here
                )
            )

            // 2. Update or Create Profile
            val existingProfile = bluetoothDao.getProfileByAddress(device.address)
            if (existingProfile == null) {
                bluetoothDao.insertProfile(
                    BluetoothProfileEntity(
                        btAddress = device.address,
                        name = device.name,
                        deviceType = device.deviceType,
                        firstSeen = device.timestamp,
                        lastSeen = device.timestamp,
                        confidenceScore = 0.1f // Initial low confidence
                    )
                )
            } else {
                // Heuristic: If RSSI is strong and hotspot is active, boost confidence
                val boost = if (device.rssi > -60 && _uiState.value.isHotspotEnabled) 0.05f else 0.01f
                val newScore = (existingProfile.confidenceScore + boost).coerceAtMost(1.0f)
                
                bluetoothDao.updateProfile(
                    existingProfile.copy(
                        lastSeen = device.timestamp,
                        confidenceScore = newScore,
                        name = device.name ?: existingProfile.name
                    )
                )
            }
        }

        // Update UI list
        _uiState.update { state ->
            val updatedList = state.nearbyBluetoothDevices.toMutableList()
            val index = updatedList.indexOfFirst { it.address == device.address }
            if (index != -1) {
                updatedList[index] = device
            } else {
                updatedList.add(device)
            }
            state.copy(nearbyBluetoothDevices = updatedList.sortedByDescending { it.rssi })
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
