package com.example.fyp_hotspot_mobility
import com.example.fyp_hotspot_mobility.viewmodel.HotspotViewModel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

import com.example.fyp_hotspot_mobility.ui.navigation.HotspotManagerApp
import com.example.fyp_hotspot_mobility.ui.theme.HotspotManagerTheme

// val devices = DeviceScanner.getConnectedDevices()

class MainActivity : ComponentActivity() {
    // Initialize your ViewModel
    private val viewModel: HotspotViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Starting refresh loop
        viewModel.startAutoRefresh()

        setContent {
            HotspotManagerTheme {
                HotspotManagerApp(viewModel)
            }
        }
    }
}

@Composable
fun HotspotScreen(viewModel: HotspotViewModel) {
    // This line links your UI to the data in the ViewModel
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hotspot Status", 
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        // This will show the SSID we grabbed with the Regex!
        Text(text = "SSID: ${uiState.ssid ?: "Not Found"}")

        // This updates every 5 seconds from your loop
        Text(text = "Connected Devices: ${uiState.devices.size}")
    }
}
