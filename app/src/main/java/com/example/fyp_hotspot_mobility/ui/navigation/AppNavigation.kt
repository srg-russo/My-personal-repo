package com.example.fyp_hotspot_mobility.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import com.example.fyp_hotspot_mobility.ui.components.DeviceDetailBottomSheet
import com.example.fyp_hotspot_mobility.ui.components.TopStatusBar
import com.example.fyp_hotspot_mobility.ui.screens.BandwidthScreen
import com.example.fyp_hotspot_mobility.ui.screens.BlockManagerScreen
import com.example.fyp_hotspot_mobility.ui.screens.DeviceListScreen
import com.example.fyp_hotspot_mobility.viewmodel.HotspotViewModel

private sealed class HotspotTab(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Devices : HotspotTab("devices", "Devices", Icons.Rounded.Devices)
    object Bandwidth : HotspotTab("bandwidth", "Bandwidth", Icons.Rounded.Speed)
    object Block : HotspotTab("block", "Block", Icons.Rounded.Block)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotManagerApp(
    viewModel: HotspotViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val navController = rememberNavController()
    val sheetState = rememberModalBottomSheetState()

    var selectedDevice by remember { mutableStateOf<ConnectedDevice?>(null) }

    val onDeviceSelected: (ConnectedDevice) -> Unit = { device ->
        selectedDevice = device
    }

    val onDismissSheet = {
        selectedDevice = null
    }

    LaunchedEffect(uiState.devices) {
        selectedDevice?.let { selected ->
            selectedDevice = uiState.devices.find { it.id == selected.id }
        }
    }

    Scaffold(
        topBar = {
            TopStatusBar(
                ssid = uiState.ssid,
                isHotspotEnabled = uiState.isHotspotEnabled,
                connectedCount = uiState.connectedCount,
                isScanning = uiState.isScanning,
                hasScannedAtLeastOnce = uiState.hasScannedAtLeastOnce,
                onScanRequested = viewModel::scanDevices
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val tabs = listOf(HotspotTab.Devices, HotspotTab.Bandwidth, HotspotTab.Block)
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HotspotTab.Devices.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(HotspotTab.Devices.route) {
                DeviceListScreen(
                    devices = uiState.devices,
                    isScanning = uiState.isScanning,
                    hasScannedAtLeastOnce = uiState.hasScannedAtLeastOnce,
                    onRefresh = viewModel::scanDevices,
                    onDeviceSelected = onDeviceSelected
                )
            }
            composable(HotspotTab.Bandwidth.route) {
                BandwidthScreen(
                    devices = uiState.devices,
                    onDeviceSelected = onDeviceSelected
                )
            }
            composable(HotspotTab.Block.route) {
                BlockManagerScreen(
                    devices = uiState.devices,
                    onBlock = viewModel::blockDevice,
                    onUnblock = viewModel::unblockDevice,
                    onDeviceSelected = onDeviceSelected
                )
            }
        }
    }

    if (selectedDevice != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissSheet,
            sheetState = sheetState,
        ) {
            selectedDevice?.let { device ->
                DeviceDetailBottomSheet(
                    device = device,
                    onDismiss = onDismissSheet,
                    onNicknameChanged = { viewModel.updateDeviceNickname(device.id, it) },
                    onBandwidthLimitChanged = { viewModel.setBandwidthLimit(device.id, it) },
                    onBlockToggle = {
                        if (device.isBlocked) viewModel.unblockDevice(device.id)
                        else viewModel.blockDevice(device.id)
                        onDismissSheet()
                    },
                )
            }
        }
    }
}
