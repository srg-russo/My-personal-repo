package com.example.fyp_hotspot_mobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.fyp_hotspot_mobility.ui.theme.Fyp_hotspot_mobilityTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Inet4Address
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initialize blocklist manager
        BlocklistManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            Fyp_hotspot_mobilityTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HotspotDeviceScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotDeviceScreen() {
    val devices = remember { mutableStateListOf<HotspotScanner.DeviceInfo>() }
    var isScanning by remember { mutableStateOf(false) }
    var lastScannedAt by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val limits by DataLimitManager.limitsFlow.collectAsState()
    val usage by DataLimitManager.usageFlow.collectAsState()

    var showSetLimitDialog by remember { mutableStateOf(false) }
    var dialogTargetMac by remember { mutableStateOf<String?>(null) }
    var dialogTargetName by remember { mutableStateOf<String?>(null) }

    suspend fun getLocalIpv4Address(): String? = withContext(Dispatchers.IO) {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return@withContext addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
        }
        null
    }

    suspend fun doScan(context: android.content.Context) {
        isScanning = true
        try {
            val localIp = getLocalIpv4Address()
            val prefix = if (localIp != null && localIp.contains('.')) {
                localIp.substring(0, localIp.lastIndexOf('.') + 1)
            } else {
                "192.168.43." // fallback common hotspot subnet
            }
            val timeoutMs = 200
            val results = coroutineScope {
                (1..254).map { i ->
                    async(Dispatchers.IO) {
                        val ip = "$prefix$i"
                        try {
                            val inet = InetAddress.getByName(ip)
                            if (inet.isReachable(timeoutMs)) {
                                // attempt to resolve hostname via nslookup (falls back to Java reverse lookup)
                                val host = try {
                                    HotspotScanner.resolveHostname(ip, timeoutMs.toLong())
                                } catch (_: Exception) {
                                    // final fallback to inet.hostName if resolve fails
                                    try {
                                        val hn = inet.hostName
                                        if (hn != ip) hn else null
                                    } catch (_: Exception) { null }
                                }
                                // Use IP as mac placeholder so UI keys remain unique
                                HotspotScanner.DeviceInfo(ip = ip, mac = ip, hostname = host)
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.map { it.await() }.filterNotNull()
            }

            devices.clear()
            devices.addAll(results)
            // record session starts for devices we just discovered (using IP as mac placeholder)
            results.forEach { d -> SessionManager.recordSeen(d.mac) }
            lastScannedAt = java.time.LocalTime.now().withNano(0).toString()
        } catch (e: Exception) {
            // ignore
        } finally {
            isScanning = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Connected Devices") }) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(8.dp)) {
            Button(onClick = { scope.launch { doScan(context) } }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isScanning) "Scanning..." else "Scan network for active IPs")
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))

            lastScannedAt?.let { Text("Last scan: $it", modifier = Modifier.padding(vertical = 6.dp)) }

            Divider()

            val blocked by BlocklistManager.blockedFlow.collectAsState()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val visibleDevices = devices.filter { !blocked.contains(it.mac) }
                items(visibleDevices, key = { it.mac }) { d ->
                    val limit = limits[d.mac]
                    val used = usage[d.mac] ?: 0L
                    DeviceRow(device = d, limitBytes = limit, usageBytes = used,
                        onSetLimit = { mac ->
                            dialogTargetMac = mac
                            dialogTargetName = d.hostname
                            showSetLimitDialog = true
                        },
                        onAddTestUsage = { mac ->
                            // add 1MB for quick testing
                            DataLimitManager.recordUsage(mac, 1_048_576L)
                        }
                        , onClick = { dev ->
                            val intent = android.content.Intent(context, DeviceDetailsActivity::class.java)
                            intent.putExtra(DeviceDetailsActivity.EXTRA_MAC, dev.mac)
                            intent.putExtra(DeviceDetailsActivity.EXTRA_IP, dev.ip)
                            intent.putExtra(DeviceDetailsActivity.EXTRA_NAME, dev.hostname)
                            context.startActivity(intent)
                        }
                    )
                    Divider()
                }
                if (devices.isEmpty() && !isScanning) {
                    item {
                        Text("No active IPs found. Run a scan.", modifier = Modifier.padding(12.dp))
                    }
                }
            }

            if (showSetLimitDialog && dialogTargetMac != null) {
                SetLimitDialog(
                    name = dialogTargetName ?: dialogTargetMac!!,
                    onConfirm = { mb ->
                        val bytes = (mb * 1024L * 1024L)
                        DataLimitManager.setLimit(dialogTargetMac!!, bytes)
                        showSetLimitDialog = false
                        dialogTargetMac = null
                        dialogTargetName = null
                    },
                    onDismiss = {
                        showSetLimitDialog = false
                        dialogTargetMac = null
                        dialogTargetName = null
                    }
                )
            }
        }
    }
}

@Composable
fun DeviceRow(
    device: HotspotScanner.DeviceInfo,
    limitBytes: Long?,
    usageBytes: Long,
    onSetLimit: (mac: String) -> Unit,
    onAddTestUsage: (mac: String) -> Unit,
    onClick: (HotspotScanner.DeviceInfo) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
        .clickable { onClick(device) }) {
        Text(text = device.hostname ?: "(unknown)", style = MaterialTheme.typography.titleMedium)
        Text(text = "IP: ${device.ip}")
        Text(text = "MAC/IP: ${device.mac}")
        Text(text = "Usage: ${formatBytes(usageBytes)}")
        Text(text = "Limit: ${limitBytes?.let { formatBytes(it) } ?: "(none)"}")

        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { onSetLimit(device.mac) }) {
                Text("Set Limit")
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
            Button(onClick = { onAddTestUsage(device.mac) }) {
                Text("Add 1MB")
            }
        }
    }
}

@Composable
fun SetLimitDialog(name: String, onConfirm: (mb: Long) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val mb = text.toLongOrNull() ?: 0L
                onConfirm(mb)
            }) {
                Text("Set")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Set data limit for $name") },
        text = {
            Column {
                Text("Enter limit in megabytes (MB) for this session:")
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("MB") })
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HotspotDevicePreview() {
    Fyp_hotspot_mobilityTheme {
        HotspotDeviceScreen()
    }
}
