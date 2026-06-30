package com.example.fyp_hotspot_mobility.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    onClick: () -> Unit = {},
) {
    val statusColor = if (device.isBlocked) Color(0xFF9E9E9E) else Color(0xFF4CAF50)
    val cardAlpha = if (device.isBlocked) 0.55f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .then(Modifier.alpha(cardAlpha))) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
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
        }
    }
}

