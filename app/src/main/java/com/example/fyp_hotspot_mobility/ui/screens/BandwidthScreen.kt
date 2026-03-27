package com.example.fyp_hotspot_mobility.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import com.example.fyp_hotspot_mobility.ui.components.DeviceCard

@Composable
fun BandwidthScreen(
    devices: List<ConnectedDevice>,
    onDeviceSelected: (ConnectedDevice) -> Unit
) {
    if (devices.isEmpty()) {
        Text(
            text = "No devices currently connected.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(devices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                showBandwidth = true,
                onClick = { onDeviceSelected(device) }
            )
        }
    }
}
