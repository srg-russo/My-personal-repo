package com.example.fyp_hotspot_mobility.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import com.example.fyp_hotspot_mobility.ui.components.DeviceCard

@Composable
fun DeviceListScreen(
    devices: List<ConnectedDevice>,
    isScanning: Boolean,
    hasScannedAtLeastOnce: Boolean,
    onRefresh: () -> Unit,
    onDeviceSelected: (ConnectedDevice) -> Unit,
) {
    if (!isScanning && !hasScannedAtLeastOnce) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text("Scan for connected devices")
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (devices.isEmpty() && hasScannedAtLeastOnce && !isScanning) {
                    item {
                        Text(
                            text = "No connected devices found.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        onClick = { onDeviceSelected(device) }
                    )
                }
            }
        }
    }
}
