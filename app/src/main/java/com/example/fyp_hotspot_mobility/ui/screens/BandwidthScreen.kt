package com.example.fyp_hotspot_mobility.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fyp_hotspot_mobility.model.ConnectedDevice

@Composable
fun BandwidthScreen(
    devices: List<ConnectedDevice>,
    onDeviceSelected: (ConnectedDevice) -> Unit
) {
    val totalUsage = devices.sumOf { it.usageMb.toDouble() }.toFloat()

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Collective Usage",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${"%.2f".format(totalUsage)} MB",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Monitoring ${devices.size} active devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No devices currently connected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(devices, key = { it.id }) { device ->
                BandwidthDeviceRow(
                    device = device,
                    onClick = { onDeviceSelected(device) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun BandwidthDeviceRow(
    device: ConnectedDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBlocked = device.isBlocked
    val cardAlpha = if (isBlocked) 0.55f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(cardAlpha)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.hostname.ifBlank { "Unknown Device" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isBlocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.Unspecified
                        )
                        if (isBlocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "BLOCKED",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = device.ipAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${"%.1f".format(device.usageMb)} MB",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                    if (device.dataLimitMb != null) {
                        Text(
                            text = "Limit: ${device.dataLimitMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No limit set",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            if (device.dataLimitMb != null) {
                val progress = (device.usageMb / device.dataLimitMb).coerceIn(0f, 1f)
                val color = when {
                    isBlocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    progress > 0.9f -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(MaterialTheme.shapes.extraSmall),
                        color = color,
                        trackColor = color.copy(alpha = 0.2f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% used",
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                        val remaining = (device.dataLimitMb - device.usageMb).coerceAtLeast(0f)
                        Text(
                            text = "${"%.1f".format(remaining)} MB left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
