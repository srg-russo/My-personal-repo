
package com.example.fyp_hotspot_mobility.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fyp_hotspot_mobility.model.ConnectedDevice

@Composable
fun DeviceDetailBottomSheet(
    device: ConnectedDevice,
    onDismiss: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    onBandwidthLimitChanged: (Int?) -> Unit,
    onBlockToggle: () -> Unit,
) {
    var nickname by remember { mutableStateOf(device.hostname) }
    var limitKbps by remember { mutableStateOf(device.bandwidthLimitKbps ?: 0) }

    LaunchedEffect(device.id) {
        nickname = device.hostname
        limitKbps = device.bandwidthLimitKbps ?: 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it; onNicknameChanged(it) },
            label = { Text("Device name") },
            modifier = Modifier.fillMaxWidth()
        )

        Divider()

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("IP Address", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Text(device.ipAddress, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
            Column {
                Text("MAC", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Text(device.id, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }

        Divider()

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Download", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Text("${"%.1f".format(device.downloadSpeed)} KB/s", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
            Column {
                Text("Upload", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Text("${"%.1f".format(device.uploadSpeed)} KB/s", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }

        Divider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bandwidth cap (Kbps)", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            Slider(
                value = limitKbps.toFloat(),
                onValueChange = { limitKbps = it.toInt(); onBandwidthLimitChanged(if (it.toInt() == 0) null else it.toInt()) },
                valueRange = 0f..10_000f,
                steps = 10
            )
            Text(
                text = if (limitKbps <= 0) "Unlimited" else "${limitKbps} Kbps",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = onBlockToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (device.isBlocked) Color(0xFF388E3C) else Color(0xFFD32F2F),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (device.isBlocked) "Unblock Device" else "Block Device")
        }

        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text("Dismiss")
        }
    }
}
