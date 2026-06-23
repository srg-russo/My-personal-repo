package com.example.fyp_hotspot_mobility.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.fyp_hotspot_mobility.model.ConnectedDevice
import com.example.fyp_hotspot_mobility.ui.components.DeviceDetailBottomSheet
import com.example.fyp_hotspot_mobility.ui.components.TopStatusBar
import com.example.fyp_hotspot_mobility.ui.screens.BandwidthScreen
import com.example.fyp_hotspot_mobility.ui.screens.BlockManagerScreen
import com.example.fyp_hotspot_mobility.ui.screens.DeviceListScreen
import com.example.fyp_hotspot_mobility.viewmodel.HotspotViewModel

sealed class HostwatchTab(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Devices : HostwatchTab("devices", "Devices", Icons.Rounded.Devices)
    object Bandwidth : HostwatchTab("bandwidth", "Bandwidth", Icons.Rounded.Speed)
    object Status : HostwatchTab("block", "Status", Icons.Rounded.Block)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostwatchApp(
    viewModel: HotspotViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val navController = rememberNavController()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var selectedDevice by remember { mutableStateOf<ConnectedDevice?>(null) }

    val onDeviceSelected: (ConnectedDevice) -> Unit = { device ->
        selectedDevice = device
    }

    val onDismissSheet = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                selectedDevice = null
            }
        }
        Unit
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val totalUsage = uiState.devices.sumOf { it.usageMb.toDouble() }.toFloat()

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
                onScanRequested = viewModel::scanDevices,
                showRefreshButton = currentRoute == HostwatchTab.Devices.route,
                totalBandwidth = if (currentRoute == HostwatchTab.Bandwidth.route) totalUsage else null,
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val tabs = listOf(HostwatchTab.Devices, HostwatchTab.Bandwidth, HostwatchTab.Status)
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
            startDestination = HostwatchTab.Devices.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(400)) + slideInHorizontally(animationSpec = tween(400)) { it }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(400)) + slideOutHorizontally(animationSpec = tween(400)) { -it }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(400)) + slideInHorizontally(animationSpec = tween(400)) { -it }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(400)) + slideOutHorizontally(animationSpec = tween(400)) { it }
            }
        ) {
            composable(HostwatchTab.Devices.route) {
                DeviceListScreen(
                    devices = uiState.devices,
                    isScanning = uiState.isScanning,
                    hasScannedAtLeastOnce = uiState.hasScannedAtLeastOnce,
                    onRefresh = viewModel::scanDevices,
                    onDeviceSelected = onDeviceSelected
                )
            }
            composable(HostwatchTab.Bandwidth.route) {
                BandwidthScreen(
                    devices = uiState.devices,
                    onDeviceSelected = onDeviceSelected
                )
            }
            composable(HostwatchTab.Status.route) {
                BlockManagerScreen(
                    devices = uiState.devices,
                    onBlock = viewModel::blockDevice,
                    onUnblock = viewModel::unblockDevice
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
                    onDataLimitChanged = { viewModel.setDataLimit(device.id, it) },
                    onBlock = { viewModel.blockDevice(device.id) },
                    onUnblock = { viewModel.unblockDevice(device.id) },
                    showLimitEditor = currentRoute == HostwatchTab.Bandwidth.route,
                    showBlockOptions = currentRoute == HostwatchTab.Devices.route
                )
            }
        }
    }

    // Alert for exceeded limits
    val alertMessage = uiState.limitExceededAlert
    if (alertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text("Bandwidth Limit Exceeded") },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) {
                    Text("OK")
                }
            }
        )
    }
}
