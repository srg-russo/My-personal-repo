package com.example.fyp_hotspot_mobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fyp_hotspot_mobility.ui.navigation.HotspotManagerApp
import com.example.fyp_hotspot_mobility.ui.theme.HotspotManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HotspotManagerTheme {
                HotspotManagerApp()
            }
        }
    }
}
