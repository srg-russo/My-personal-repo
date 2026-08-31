package com.example.fyp_hotspot_mobility.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
                        text = "Hostwatch",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$connectedCount connected",
                            style = MaterialTheme.typography.bodySmall
                        )
                        AnimatedVisibility(
                            visible = totalBandwidth != null,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Total: ${"%.1f".format(totalBandwidth ?: 0f)} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            AnimatedContent(
                targetState = isScanning,
                label = "ScanProgress"
            ) { scanning ->
                if (showRefreshButton) {
                    if (scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (hasScannedAtLeastOnce) {
                        IconButton(onClick = onScanRequested) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Rescan")
                        }
                    }
                }
            }
        }
    )
}
