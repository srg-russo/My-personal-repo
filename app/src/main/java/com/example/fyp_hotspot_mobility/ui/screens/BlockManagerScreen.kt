package com.example.fyp_hotspot_mobility.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    onUnblock: (String) -> Unit,
    onDeviceSelected: (ConnectedDevice) -> Unit
) {
    val connected = devices.filter { !it.isBlocked }
    val blocked = devices.filter { it.isBlocked }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { SectionHeader(title = "Connected") }

        if (connected.isEmpty()) {
            item {
                Text(
                    text = "No connected devices.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            itemsIndexed(connected) { _, device ->
                DeviceRowWithAction(
                    device = device,
                    buttonText = "Block",
                    buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onButtonClick = { onBlock(device.id) },
                    onClick = { onDeviceSelected(device) }
                )
            }
        }

        item { SectionHeader(title = "Blocked") }

        if (blocked.isEmpty()) {
            item {
                Text(
                    text = "No blocked devices.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            itemsIndexed(blocked) { _, device ->
                DeviceRowWithAction(
                    device = device,
                    buttonText = "Unblock",
                    buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    onButtonClick = { onUnblock(device.id) },
                    onClick = { onDeviceSelected(device) }
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .clickable { onClick() }) {
            Text(text = device.hostname.ifBlank { "Unknown Device" }, style = MaterialTheme.typography.titleMedium)
            Text(text = device.ipAddress, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onButtonClick, colors = buttonColors) {
            Text(buttonText)
        }
    }
}
