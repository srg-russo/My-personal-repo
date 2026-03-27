package com.example.fyp_hotspot_mobility.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetState
import androidx.compose.material3.ModalBottomSheetValue
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import com.example.fyp_hotspot_mobility.ui.components.DeviceDetailBottomSheet
import com.example.fyp_hotspot_mobility.ui.components.TopStatusBar
import com.example.fyp_hotspot_mobility.ui.screens.BandwidthScreen
import com.example.fyp_hotspot_mobility.ui.screens.BlockManagerScreen
import com.example.fyp_hotspot_mobility.ui.screens.DeviceListScreen
import com.example.fyp_hotspot_mobility.viewmodel.HotspotViewModel

private sealed interface HotspotTab {
    val route: String
    val title: String

    object Devices : HotspotTab {
        override val route = "devices"
        override val title = "Devices"
    }

    object Bandwidth : HotspotTab {
        override val route = "bandwidth"
        override val title = "Bandwidth"
    }

    object Block : HotspotTab {
        override val route = "block"
        override val title = "Block"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotManagerApp(
    viewModel: HotspotViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.startAutoRefresh(lifecycleOwner.lifecycle)
    }

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
        // Keep sheet updated when we update device list (for example, a device becomes blocked)
        selectedDevice?.let { selected ->
            selectedDevice = uiState.devices.find { it.id == selected.id }
        }
    }

    LaunchedEffect(selectedDevice) {
        if (selectedDevice != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissSheet,
        sheetState = sheetState
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
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopStatusBar(
                ssid = uiState.ssid,
                isHotspotEnabled = uiState.isHotspotEnabled,
                connectedCount = uiState.connectedCount,
                isScanning = uiState.isScanning,
                onScanRequested = viewModel::scanDevices
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                listOf(HotspotTab.Devices, HotspotTab.Bandwidth, HotspotTab.Block).forEach { tab ->
                    NavigationBarItem(
                        icon = { Text(tab.title.take(1)) },
                        label = { Text(tab.title) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HotspotTab.Devices.route) {
                DeviceListScreen(
                    devices = uiState.devices,
                    isScanning = uiState.isScanning,
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
}
