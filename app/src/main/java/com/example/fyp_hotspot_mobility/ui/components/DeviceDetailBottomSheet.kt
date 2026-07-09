package com.example.fyp_hotspot_mobility.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import com.example.fyp_hotspot_mobility.data.local.entity.UsageLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceDetailBottomSheet(
    device: ConnectedDevice,
    usageLogs: List<UsageLogEntity> = emptyList(),
    onDismiss: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    onDataLimitChanged: (Int?) -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onClearHistory: () -> Unit = {},
    showLimitEditor: Boolean = false,
    showBlockOptions: Boolean = false,
) {
    var nickname by remember { mutableStateOf(device.hostname) }
    var limitInput by remember { mutableStateOf(device.dataLimitMb?.toString() ?: "") }

    LaunchedEffect(device.id) {
        nickname = device.hostname
        limitInput = device.dataLimitMb?.toString() ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Device Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it; onNicknameChanged(it) },
            label = { Text("Device Nickname") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("IP Address", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(device.ipAddress, style = MaterialTheme.typography.bodyLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Hardware ID", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(device.id, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Data Usage Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            
            val isExceeded = device.dataLimitMb != null && device.usageMb > device.dataLimitMb
            val usageText = "${"%.2f".format(device.usageMb)} MB used"
            val limitText = if (device.dataLimitMb != null) " of ${device.dataLimitMb} MB limit" else " (No limit set)"
            
            Text(
                text = if (isExceeded) "$usageText (EXCEEDED)" else usageText + limitText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            if (device.dataLimitMb != null) {
                val progress = (device.usageMb / device.dataLimitMb).coerceIn(0f, 1f)
                val remaining = (device.dataLimitMb - device.usageMb).coerceAtLeast(0f)
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (isExceeded) MaterialTheme.colorScheme.error else if (progress > 0.8f) Color(0xFFFFA000) else MaterialTheme.colorScheme.primary
                )
                
                if (isExceeded) {
                    Text(
                        text = "Device has used ${"%.1f".format(device.usageMb - device.dataLimitMb)} MB over the limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "${"%.1f".format(remaining)} MB remaining before cap",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (remaining < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showBlockOptions) {
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Access Control", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    Button(
                        onClick = { if (device.isBlocked) onUnblock() else onBlock() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (device.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (device.isBlocked) "Unblock" else "Block")
                    }
                }
            }
        }

        if (showLimitEditor) {
            HorizontalDivider()
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set Data Limit (MB)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            limitInput = input
                            onDataLimitChanged(input.toIntOrNull())
                        }
                    },
                    placeholder = { Text("Enter limit in MB") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("MB") },
                    supportingText = { Text("Set to empty to remove limit") }
                )
            }
        }

        if (usageLogs.isNotEmpty()) {
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Session History",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onClearHistory) {
                        Text("Clear History", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                usageLogs.take(10).forEach { log ->
                    val date = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(date, style = MaterialTheme.typography.bodyMedium)
                        Text("${"%.2f".format(log.usageMb)} MB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Close")
        }
    }
}
