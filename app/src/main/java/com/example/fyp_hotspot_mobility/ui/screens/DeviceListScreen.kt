package com.example.fyp_hotspot_mobility.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = devices.isEmpty() && hasScannedAtLeastOnce && !isScanning) {
            Text(
                text = "No devices found. Tap refresh to scan for connected devices.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
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

        if (!isScanning && !hasScannedAtLeastOnce) {
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Scan for connected devices")
            }
        }
    }
}
