package com.example.fyp_hotspot_mobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.fyp_hotspot_mobility.ui.navigation.HotspotManagerApp
import com.example.fyp_hotspot_mobility.ui.theme.HotspotManagerTheme
import com.example.fyp_hotspot_mobility.viewmodel.HotspotViewModel

class MainActivity : ComponentActivity() {
    // Initialize your ViewModel
    private val viewModel: HotspotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HotspotManagerTheme {
                HotspotManagerApp(viewModel)
            }
        }
    }
}
