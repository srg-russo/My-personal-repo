package com.example.fyp_hotspot_mobility.agent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        setupAgent()
        checkVpnPermission()
    }

    private val vpnRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private fun checkVpnPermission() {
        val intent = android.net.VpnService.prepare(this)
        if (intent != null) {
            vpnRequestLauncher.launch(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissionsAndSetup()
        checkVpnPermission()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hostwatch Agent is Active")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Closing in a few seconds...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndSetup() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            setupAgent()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun setupAgent() {
        // 1. Initial Start
        val intent = Intent(this, AgentService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // 2. SET THE SYSTEM TRAP: Wake me up on any Wi-Fi activity forever
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            connectivityManager.registerNetworkCallback(
                AgentService.getNetworkRequest(),
                AgentService.getPendingIntent(this)
            )
        } catch (e: Exception) { }

        // 3. Auto-close the activity after a delay
        lifecycleScope.launch {
            delay(5000)
            finish()
        }
    }
}
