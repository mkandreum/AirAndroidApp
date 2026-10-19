package com.xyon.airandroid.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xyon.airandroid.model.ConnectionState
import com.xyon.airandroid.model.PeerDevice
import com.xyon.airandroid.model.Role
import com.xyon.airandroid.model.SelectedFile
import com.xyon.airandroid.model.TransferUiState
import com.xyon.airandroid.ui.components.EmptyState
import com.xyon.airandroid.ui.components.FileRow
import com.xyon.airandroid.ui.components.GlassCard
import com.xyon.airandroid.ui.components.HeroCard
import com.xyon.airandroid.ui.components.HintCard
import com.xyon.airandroid.ui.components.PeerCard
import com.xyon.airandroid.ui.components.PinInput
import com.xyon.airandroid.ui.components.PrimaryActionButton
import com.xyon.airandroid.ui.components.ProgressCard
import com.xyon.airandroid.ui.components.RadarAnimation
import com.xyon.airandroid.ui.components.RemoveFileButton
import com.xyon.airandroid.ui.components.ResultCard
import com.xyon.airandroid.ui.components.ScreenHeader
import com.xyon.airandroid.ui.components.SecondaryActionButton
import com.xyon.airandroid.ui.components.SectionHeader
import com.xyon.airandroid.ui.components.StatusPill
import com.xyon.airandroid.ui.theme.Border
import com.xyon.airandroid.ui.theme.Error
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.TextMuted
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextSecondary
import com.xyon.airandroid.ui.theme.Warning
import com.xyon.airandroid.util.formatBytes
import com.xyon.airandroid.util.rememberHaptics

@Composable
fun SendScreen(
    files: List<SelectedFile>,
    peers: List<PeerDevice>,
    selectedPeer: PeerDevice?,
    pin: String,
    scanning: Boolean,
    connection: ConnectionState,
    transfer: TransferUiState,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
    nfcReaderActive: Boolean,
    onAddFiles: (List<Uri>) -> Unit,
    onRemoveFile: (Uri) -> Unit,
    onClearFiles: () -> Unit,
    onScan: () -> Unit,
    onSelectPeer: (PeerDevice) -> Unit,
    onPinChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onNfcRead: () -> Unit,
    onStopNfc: () -> Unit,
    onDismissStatus: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val busy = transfer is TransferUiState.Connecting || transfer is TransferUiState.Sending
    val canSend = files.isNotEmpty() &&
        selectedPeer != null &&
        pin.length == 6 &&
        !busy
    val totalSize = files.sumOf { it.size }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris -> onAddFiles(uris) }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> onAddFiles(uris) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenHeader(
                title = "Enviar",
                subtitle = "Rápido, cifrado y sin internet",
                onSettings = {
                    haptics.tick()
                    onOpenSettings()
                },
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    label = when {
                        scanning -> "Buscando"
                        connection.groupReady -> "Wi-Fi Direct listo"
                        else -> "Wi-Fi Direct"
                    },
                    active = connection.groupReady || scanning,
                )
                StatusPill(label = "Sin internet", active = true, color = Primary)
            }
        }

        item {
            HeroCard(
                title = "Envío directo",
                text = "Wi-Fi Direct + cifrado AES-256. Tus archivos no pasan por ningún servidor.",
                icon = Icons.Rounded.Bolt,
            )
        }

        if (!connection.supported) {
            item {
                HintCard(
                    title = "Wi-Fi Direct no disponible",
                    text = "Este móvil no admite Wi-Fi Direct. Prueba con otro dispositivo.",
                    tone = Error,
                    icon = Icons.Rounded.Devices,
                )
            }
        } else if (!connection.wifiP2pEnabled && connection.role == Role.SENDER) {
            item {
                HintCard(
                    title = "Wi-Fi desactivado",
                    text = "Activa Wi-Fi para buscar y conectar con el otro móvil.",
                    actionLabel = "Activar",
                    onAction = onOpenWifiSettings,
                    tone = Warning,
                )
            }
        }

        item {
            SectionHeader(
                step = "01",
                title = "Elige archivos",
                action = if (files.isNotEmpty()) {
                    {
                        TextButton(onClick = {
                            haptics.tick()
                            onClearFiles()
                        }) {
                            Text("Limpiar", color = TextSecondary)
                        }
                    }
                } else {
                    null
                },
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryActionButton(
                    text = "Fotos y vídeos",
                    icon = Icons.Rounded.PhotoLibrary,
                    onClick = {
                        haptics.tick()
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                SecondaryActionButton(
                    text = "Archivos",
                    icon = Icons.Rounded.Folder,
                    onClick = {
                        haptics.tick()
                        filePicker.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = files.isEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                GlassCard {
                    EmptyState(
                        icon = Icons.Rounded.UploadFile,
                        title = "Aún no hay archivos",
                        text = "Elige fotos, vídeos o cualquier archivo. Puedes seleccionar varios a la vez.",
                    )
                }
            }
            AnimatedVisibility(
                visible = files.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (files.size == 1) "1 archivo" else "${files.size} archivos",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = formatBytes(totalSize),
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Border)
                    files.take(8).forEach { file ->
                        FileRow(
                            name = file.name,
                            size = file.size,
                            mime = file.mimeType,
                            trailing = {
                                RemoveFileButton {
                                    haptics.tick()
                                    onRemoveFile(file.uri)
                                }
                            },
                        )
                    }
                    if (files.size > 8) {
                        Text(
                            text = "+${files.size - 8} archivos más",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                step = "02",
                title = "Busca el receptor",
                action = {
                    TextButton(onClick = {
                        haptics.tick()
                        onScan()
                    }) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = Primary,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(if (scanning) "Buscando…" else "Buscar", color = Primary)
                    }
                },
            )
        }

        item {
            when {
                scanning -> GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        RadarAnimation(active = true, modifier = Modifier.size(126.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Buscando dispositivos",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                "Activa Recibir en el otro móvil para que aparezca aquí.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                peers.isEmpty() -> GlassCard {
                    EmptyState(
                        icon = Icons.Rounded.Devices,
                        title = "No hay dispositivos todavía",
                        text = "En el otro móvil abre AirAndroid, entra en Recibir y pulsa Activar recepción.",
                    )
                }

                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    peers.take(6).forEach { peer ->
                        PeerCard(
                            peer = peer,
                            selected = selectedPeer?.address == peer.address,
                            onClick = { onSelectPeer(peer) },
                        )
                    }
                    if (peers.size > 6) {
                        Text(
                            text = "+${peers.size - 6} dispositivos más",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                        )
                    }
                    if (selectedPeer != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Primary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Seleccionado: ${selectedPeer.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(step = "03", title = "Código del receptor")
        }

        item {
            GlassCard {
                Text(
                    text = "El otro móvil muestra un código de 6 dígitos. Introdúcelo aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                PinInput(
                    pin = pin,
                    onPinChange = onPinChange,
                    enabled = !busy,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "El código no se envía por la red. Solo desbloquea el cifrado.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                }
                Spacer(Modifier.height(14.dp))

                when {
                    nfcReaderActive -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Primary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Primary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Acerca el móvil del receptor…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onStopNfc) {
                            Text("Cancelar", color = TextSecondary)
                        }
                    }

                    nfcAvailable -> SecondaryActionButton(
                        text = if (nfcEnabled) {
                            "Leer código por NFC"
                        } else {
                            "NFC desactivado"
                        },
                        icon = Icons.Rounded.Nfc,
                        enabled = nfcEnabled,
                        onClick = {
                            haptics.tick()
                            onNfcRead()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    else -> Text(
                        text = "NFC no disponible en este móvil. Escribe el código manualmente.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                }
            }
        }

        item {
            when (transfer) {
                is TransferUiState.Connecting -> GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Primary,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Conectando",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                transfer.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SecondaryActionButton(
                        text = "Cancelar",
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is TransferUiState.Sending -> ProgressCard(
                    title = if (transfer.progress.fileCount > 0) {
                        "Enviando ${transfer.progress.fileIndex} de ${transfer.progress.fileCount}"
                    } else {
                        "Enviando"
                    },
                    progress = transfer.progress,
                    onCancel = onCancel,
                )

                is TransferUiState.Completed -> ResultCard(
                    success = true,
                    title = "Enviado",
                    text = buildString {
                        append(
                            if (transfer.fileCount == 1) {
                                "1 archivo"
                            } else {
                                "${transfer.fileCount} archivos"
                            },
                        )
                        append(" · ")
                        append(formatBytes(transfer.totalBytes))
                    },
                    onDismiss = onDismissStatus,
                )

                is TransferUiState.Failed -> ResultCard(
                    success = false,
                    title = "No se pudo enviar",
                    text = transfer.message,
                    onDismiss = onDismissStatus,
                )

                else -> PrimaryActionButton(
                    text = if (files.size == 1) {
                        "Enviar archivo"
                    } else {
                        "Enviar ${files.size} archivos"
                    },
                    icon = Icons.AutoMirrored.Rounded.Send,
                    enabled = canSend,
                    onClick = {
                        haptics.tick()
                        onSend()
                    },
                )
            }
        }
    }
}
