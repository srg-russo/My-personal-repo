package com.example.fyp_hotspot_mobility.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fyp_hotspot_mobility.model.ConnectedDevice

@Composable
fun BlockManagerScreen(
    devices: List<ConnectedDevice>,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit
) {
    val unblockedDevices = devices.filter { !it.isBlocked }
    val blockedDevices = devices.filter { it.isBlocked }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { SectionHeader(title = "Active Devices") }

        if (unblockedDevices.isEmpty()) {
            item {
                Text(
                    text = "No active devices connected.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(unblockedDevices, key = { it.id }) { device ->
                DeviceRowWithAction(
                    device = device,
                    buttonText = "Block",
                    buttonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onButtonClick = { onBlock(device.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        item { 
            Spacer(modifier = Modifier.height(48.dp))
            SectionHeader(title = "Blocked Devices") 
        }

        if (blockedDevices.isEmpty()) {
            item {
                Text(
                    text = "No blocked devices.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(blockedDevices, key = { it.id }) { device ->
                DeviceRowWithAction(
                    device = device,
                    buttonText = "Unblock",
                    buttonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    onButtonClick = { onUnblock(device.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun DeviceRowWithAction(
    device: ConnectedDevice,
    buttonText: String,
    buttonColors: androidx.compose.material3.ButtonColors,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = device.hostname.ifBlank { "Unknown Device" },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = device.ipAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Button(
            onClick = onButtonClick,
            colors = buttonColors,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(buttonText)
        }
    }
}
