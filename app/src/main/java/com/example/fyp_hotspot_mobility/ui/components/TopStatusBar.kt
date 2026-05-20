package com.example.fyp_hotspot_mobility.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopStatusBar(
    ssid: String?,
    isHotspotEnabled: Boolean,
    connectedCount: Int,
    isScanning: Boolean,
    hasScannedAtLeastOnce: Boolean,
    onScanRequested: () -> Unit,
    showRefreshButton: Boolean = true,
    totalBandwidth: Float? = null,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = if (isHotspotEnabled) Color(0xFF4CAF50) else Color(0xFFB0BEC5)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = ssid.orEmpty().ifBlank { "My Hotspot" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$connectedCount connected",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (totalBandwidth != null) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ${"%.1f".format(totalBandwidth)} MB",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        actions = {
            if (showRefreshButton) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 12.dp),
                        strokeWidth = 2.dp
                    )
                } else if (hasScannedAtLeastOnce) {
                    IconButton(onClick = onScanRequested) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Rescan")
                    }
                }
            }
        }
    )
}
