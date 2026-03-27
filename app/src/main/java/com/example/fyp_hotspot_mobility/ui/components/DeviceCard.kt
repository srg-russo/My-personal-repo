package com.example.fyp_hotspot_mobility.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fyp_hotspot_mobility.model.ConnectedDevice

@Composable
fun DeviceCard(
    device: ConnectedDevice,
    modifier: Modifier = Modifier,
    showBandwidth: Boolean = false,
    onClick: () -> Unit = {},
) {
    val statusColor = if (device.downloadSpeed + device.uploadSpeed > 0.1f) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
    val cardAlpha = if (device.isBlocked) 0.55f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .then(Modifier.alpha(cardAlpha))) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.hostname.ifBlank { "Unknown Device" }, style = MaterialTheme.typography.titleMedium)
                    Text(text = device.ipAddress, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = if (device.isBlocked) "Blocked" else "Active",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.isBlocked) Color(0xFFB00020) else MaterialTheme.colorScheme.primary
                )
            }

            if (showBandwidth) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DL: ${device.downloadSpeed.formatKbps()}", style = MaterialTheme.typography.bodySmall)
                    Text("UL: ${device.uploadSpeed.formatKbps()}", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val limit = device.bandwidthLimitKbps
                val progress = when {
                    limit == null || limit <= 0 -> 0f
                    else -> (device.downloadSpeed + device.uploadSpeed).coerceAtMost(limit.toFloat()) / limit
                }
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun Float.formatKbps(): String {
    return "${"%.1f".format(this)} KB/s"
}
