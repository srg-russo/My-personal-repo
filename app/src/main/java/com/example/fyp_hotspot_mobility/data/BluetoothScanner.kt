package com.example.fyp_hotspot_mobility.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

/**
 * Handles Bluetooth Classic and BLE scanning for device profiling.
 */
class BluetoothScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) && 
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Data class representing a discovered Bluetooth device.
     */
    data class DiscoveredBluetoothDevice(
        val address: String,
        val name: String?,
        val rssi: Int,
        val deviceType: Int, // BluetoothDevice.DEVICE_TYPE_...
        val timestamp: Long = System.currentTimeMillis(),
        val isBle: Boolean,
        val advertisementData: ByteArray? = null
    )

    @SuppressLint("MissingPermission")
    fun scanDevices(): Flow<DiscoveredBluetoothDevice> = callbackFlow {
        if (!hasRequiredPermissions()) {
            Log.e("BluetoothScanner", "Missing required permissions for scanning")
            close()
            return@callbackFlow
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            close()
            return@callbackFlow
        }

        // 1. BLE Scanning setup
        val bleScanner = bluetoothAdapter.bluetoothLeScanner
        val bleCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                trySend(
                    DiscoveredBluetoothDevice(
                        address = device.address,
                        name = result.scanRecord?.deviceName ?: device.name,
                        rssi = result.rssi,
                        deviceType = device.type,
                        isBle = true,
                        advertisementData = result.scanRecord?.bytes
                    )
                )
            }
        }

        // 2. Classic Bluetooth Scanning setup
        val classicReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = 
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        
                        device?.let {
                            trySend(
                                DiscoveredBluetoothDevice(
                                    address = it.address,
                                    name = it.name,
                                    rssi = rssi.toInt(),
                                    deviceType = it.type,
                                    isBle = false
                                )
                            )
                        }
                    }
                }
            }
        }

        // Register for Classic Bluetooth results
        context.registerReceiver(classicReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        // Start BLE Scan
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner?.startScan(null, settings, bleCallback)

        // Start Classic Scan
        bluetoothAdapter.startDiscovery()

        awaitClose {
            bleScanner?.stopScan(bleCallback)
            context.unregisterReceiver(classicReceiver)
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
        }
    }
}
