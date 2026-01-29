package com.example.fyp_hotspot_mobility

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fyp_hotspot_mobility.ui.theme.Fyp_hotspot_mobilityTheme

class DeviceDetailsActivity : ComponentActivity() {
    companion object {
        const val EXTRA_MAC = "extra_mac"
        const val EXTRA_IP = "extra_ip"
        const val EXTRA_NAME = "extra_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mac = intent.getStringExtra(EXTRA_MAC) ?: run { finish(); return }
        val ip = intent.getStringExtra(EXTRA_IP)
        val name = intent.getStringExtra(EXTRA_NAME)

        setContent {
            Fyp_hotspot_mobilityTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val usageMap by DataLimitManager.usageFlow.collectAsState()
                    val sessionStarts by SessionManager.sessionStartsFlow.collectAsState()
                    val blocked by BlocklistManager.blockedFlow.collectAsState()

                    val usage = usageMap[mac] ?: 0L
                    val start = sessionStarts[mac]
                    val durationMillis = if (start != null) System.currentTimeMillis() - start else 0L

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = name ?: "(unknown)", style = MaterialTheme.typography.titleLarge)
                        Text(text = "IP: ${ip ?: "(unknown)"}")
                        Text(text = "MAC: $mac")
                        Text(text = "Connected: ${formatMillis(durationMillis)}")
                        Text(text = "Usage: ${formatBytes(usage)}")

                        Button(onClick = {
                            if (!BlocklistManager.isBlocked(mac)) {
                                BlocklistManager.block(mac)
                                Toast.makeText(this@DeviceDetailsActivity, "Device blocked", Toast.LENGTH_SHORT).show()
                            } else {
                                BlocklistManager.unblock(mac)
                                Toast.makeText(this@DeviceDetailsActivity, "Device unblocked", Toast.LENGTH_SHORT).show()
                            }
                            val out = Intent()
                            setResult(RESULT_OK, out)
                            finish()
                        }, modifier = Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth()) {
                            Text(if (!blocked.contains(mac)) "Block device" else "Unblock device")
                        }
                    }
                }
            }
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
