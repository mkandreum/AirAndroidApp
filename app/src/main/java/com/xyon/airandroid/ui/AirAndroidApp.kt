package com.xyon.airandroid.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xyon.airandroid.model.HistoryItem
import com.xyon.airandroid.model.TransferUiState
import com.xyon.airandroid.model.UiEvent
import com.xyon.airandroid.ui.components.GlassBottomBar
import com.xyon.airandroid.ui.components.SettingsSheet
import com.xyon.airandroid.ui.screens.HistoryScreen
import com.xyon.airandroid.ui.screens.ReceiveScreen
import com.xyon.airandroid.ui.screens.SendScreen
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.Surface
import com.xyon.airandroid.ui.theme.SurfaceCard
import com.xyon.airandroid.ui.theme.SurfaceElevated
import com.xyon.airandroid.ui.theme.TextMuted
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextSecondary
import com.xyon.airandroid.util.copyToClipboard
import com.xyon.airandroid.util.findActivity
import com.xyon.airandroid.util.hasWifiPermissions
import com.xyon.airandroid.util.openFile
import com.xyon.airandroid.util.rememberHaptics
import com.xyon.airandroid.util.requiredWifiPermissions
import com.xyon.airandroid.util.shareFile
import kotlinx.coroutines.launch

@Composable
fun AirAndroidApp(viewModel: AirAndroidViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val selectedPeer by viewModel.selectedPeer.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val pin by viewModel.sendPin.collectAsStateWithLifecycle()
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()
    val receive by viewModel.receive.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    val activity = remember(context) { context.findActivity() }

    var tab by rememberSaveable { mutableStateOf(AppTab.SEND) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it }) {
            pendingAction?.invoke()
        } else {
            showPermissionDialog = true
            haptics.reject()
        }
        pendingAction = null
    }

    val ensurePermissions: (() -> Unit) -> Unit = { action ->
        if (hasWifiPermissions(context)) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(requiredWifiPermissions())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> {
                    haptics.tick()
                    snackbar.showSnackbar(event.text)
                }

                UiEvent.TransferCompleted -> haptics.confirm()
                UiEvent.Error -> haptics.reject()
                UiEvent.NeedPermissions -> showPermissionDialog = true
                UiEvent.PinRegenerated -> haptics.tick()
            }
        }
    }

    val screenBusy = transfer is TransferUiState.Connecting ||
        transfer is TransferUiState.Sending ||
        transfer is TransferUiState.Receiving

    DisposableEffect(screenBusy) {
        view.keepScreenOn = screenBusy
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = showSettings || screenBusy) {
        if (showSettings) {
            showSettings = false
        } else {
            viewModel.cancelTransfer()
        }
    }

    val openReceived: (HistoryItem) -> Unit = { item ->
        if (!openFile(context, item.uri, item.fileName)) {
            haptics.reject()
            scope.launch { snackbar.showSnackbar("No se pudo abrir el archivo") }
        }
    }
    val shareReceived: (HistoryItem) -> Unit = { item ->
        if (!shareFile(context, item.uri, item.fileName)) {
            haptics.reject()
            scope.launch { snackbar.showSnackbar("No se pudo compartir el archivo") }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary,
                    actionColor = Primary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                )
            }
        },
        bottomBar = {
            GlassBottomBar(
                current = tab,
                onSelect = { tab = it },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Surface, Surface, Color(0xFF050505)),
                    ),
                )
                .statusBarsPadding(),
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (
                        fadeIn(tween(200)) +
                            slideInHorizontally(tween(220)) { it / 16 }
                        ) togetherWith (
                        fadeOut(tween(140)) +
                            slideOutHorizontally(tween(200)) { -it / 16 }
                        )
                },
                label = "tabs",
            ) { current ->
                when (current) {
                    AppTab.SEND -> SendScreen(
                        files = files,
                        peers = peers,
                        selectedPeer = selectedPeer,
                        pin = pin,
                        scanning = scanning,
                        connection = connection,
                        transfer = transfer,
                        nfcAvailable = connection.nfcAvailable,
                        nfcEnabled = connection.nfcEnabled,
                        nfcReaderActive = connection.nfcReaderActive,
                        onAddFiles = viewModel::addFiles,
                        onRemoveFile = viewModel::removeFile,
                        onClearFiles = viewModel::clearFiles,
                        onScan = { ensurePermissions { viewModel.scanPeers() } },
                        onSelectPeer = viewModel::selectPeer,
                        onPinChange = viewModel::setSendPin,
                        onSend = { ensurePermissions { viewModel.sendSelectedFiles() } },
                        onCancel = viewModel::cancelTransfer,
                        onNfcRead = {
                            ensurePermissions {
                                activity?.let { viewModel.startNfcRead(it) }
                                    ?: run {
                                        haptics.reject()
                                        scope.launch { snackbar.showSnackbar("No se pudo abrir NFC") }
                                    }
                            }
                        },
                        onStopNfc = viewModel::stopNfcReader,
                        onDismissStatus = viewModel::dismissTransferStatus,
                        onOpenWifiSettings = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            }
                        },
                        onOpenSettings = { showSettings = true },
                    )

                    AppTab.RECEIVE -> ReceiveScreen(
                        receive = receive,
                        transfer = transfer,
                        connection = connection,
                        nfcAvailable = connection.nfcAvailable,
                        nfcEnabled = connection.nfcEnabled,
                        nfcReaderActive = connection.nfcReaderActive,
                        onStart = { ensurePermissions { viewModel.startReceive() } },
                        onStop = viewModel::stopReceive,
                        onNewPin = viewModel::regeneratePin,
                        onCopyPin = {
                            val currentPin = receive.pin.orEmpty()
                            if (currentPin.isNotBlank()) {
                                copyToClipboard(context, "PIN AirAndroid", currentPin)
                                haptics.confirm()
                                scope.launch { snackbar.showSnackbar("Código copiado") }
                            }
                        },
                        onNfcWrite = {
                            activity?.let { viewModel.startNfcWrite(it) }
                                ?: run {
                                    haptics.reject()
                                    scope.launch { snackbar.showSnackbar("No se pudo abrir NFC") }
                                }
                        },
                        onStopNfc = viewModel::stopNfcReader,
                        onCancelTransfer = viewModel::cancelTransfer,
                        onDismissStatus = viewModel::dismissTransferStatus,
                        onOpenFile = openReceived,
                        onShareFile = shareReceived,
                        onOpenWifiSettings = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            }
                        },
                        onOpenSettings = { showSettings = true },
                    )

                    AppTab.HISTORY -> HistoryScreen(
                        history = history,
                        onClear = viewModel::clearHistory,
                        onOpenFile = openReceived,
                        onShareFile = shareReceived,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            nfcAvailable = connection.nfcAvailable,
            nfcEnabled = connection.nfcEnabled,
            onSaveName = viewModel::saveDeviceName,
            onOpenWifiSettings = {
                runCatching { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            },
            onOpenNfcSettings = {
                runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
            },
            onDismiss = { showSettings = false },
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Permiso necesario") },
            text = {
                Text(
                    "Android necesita permiso de dispositivos cercanos para que AirAndroid pueda buscar y conectar móviles por Wi-Fi Direct. No se usa para rastrear tu ubicación.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                }) {
                    Text("Abrir ajustes", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Ahora no", color = TextMuted)
                }
            },
        )
    }
}
