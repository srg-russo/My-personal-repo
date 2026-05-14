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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
    onDataLimitChanged: (Int?) -> Unit,
    onBlockToggle: () -> Unit,
) {
    var nickname by remember { mutableStateOf(device.hostname) }
    var limitMb by remember { mutableStateOf(device.dataLimitMb ?: 0) }

    LaunchedEffect(device.id) {
        nickname = device.hostname
        limitMb = device.dataLimitMb ?: 0
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

        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("IP Address", style = MaterialTheme.typography.labelMedium)
                Text(device.ipAddress, style = MaterialTheme.typography.bodyMedium)
            }
            Column {
                Text("MAC", style = MaterialTheme.typography.labelMedium)
                Text(device.id, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Usage Status", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "${"%.2f".format(device.usageMb)} MB used",
                style = MaterialTheme.typography.bodyMedium,
                color = if (device.dataLimitMb != null && device.usageMb > device.dataLimitMb) 
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Data Usage Limit (MB)", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = limitMb.toFloat(),
                onValueChange = { 
                    limitMb = it.toInt()
                    onDataLimitChanged(if (it.toInt() == 0) null else it.toInt()) 
                },
                valueRange = 0f..500f,
                steps = 10
            )
            Text(
                text = if (limitMb <= 0) "No limit" else "$limitMb MB",
                style = MaterialTheme.typography.bodyMedium
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
