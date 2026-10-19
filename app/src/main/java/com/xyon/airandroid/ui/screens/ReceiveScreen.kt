package com.xyon.airandroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xyon.airandroid.model.ConnectionState
import com.xyon.airandroid.model.HistoryItem
import com.xyon.airandroid.model.ReceiveUiState
import com.xyon.airandroid.model.TransferUiState
import com.xyon.airandroid.ui.components.EmptyState
import com.xyon.airandroid.ui.components.FileRow
import com.xyon.airandroid.ui.components.GlassCard
import com.xyon.airandroid.ui.components.HeroCard
import com.xyon.airandroid.ui.components.HintCard
import com.xyon.airandroid.ui.components.PrimaryActionButton
import com.xyon.airandroid.ui.components.ProgressCard
import com.xyon.airandroid.ui.components.RadarAnimation
import com.xyon.airandroid.ui.components.ResultCard
import com.xyon.airandroid.ui.components.ScreenHeader
import com.xyon.airandroid.ui.components.SecondaryActionButton
import com.xyon.airandroid.ui.components.SectionHeader
import com.xyon.airandroid.ui.components.StatusPill
import com.xyon.airandroid.ui.theme.Border
import com.xyon.airandroid.ui.theme.Error
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.SurfaceElevated
import com.xyon.airandroid.ui.theme.TextMuted
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextSecondary
import com.xyon.airandroid.ui.theme.Warning
import com.xyon.airandroid.util.formatBytes
import com.xyon.airandroid.util.formatPin
import com.xyon.airandroid.util.rememberHaptics

@Composable
fun ReceiveScreen(
    receive: ReceiveUiState,
    transfer: TransferUiState,
    connection: ConnectionState,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
    nfcReaderActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onNewPin: () -> Unit,
    onCopyPin: () -> Unit,
    onNfcWrite: () -> Unit,
    onStopNfc: () -> Unit,
    onCancelTransfer: () -> Unit,
    onDismissStatus: () -> Unit,
    onOpenFile: (HistoryItem) -> Unit,
    onShareFile: (HistoryItem) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val pin = receive.pin.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenHeader(
                title = "Recibir",
                subtitle = "Sin cables, sin cuentas, sin internet",
                onSettings = {
                    haptics.tick()
                    onOpenSettings()
                },
            )
        }

        if (!receive.active) {
            item {
                HeroCard(
                    title = "Recibe al instante",
                    text = "Activa la recepción, comparte el código y el emisor envía sus archivos directamente.",
                    icon = Icons.Rounded.Download,
                )
            }
            item {
                GlassCard {
                    StepRow("1", "Activa la recepción", "AirAndroid crea una red Wi-Fi Direct privada.")
                    StepRow("2", "Comparte el código", "El emisor introduce el PIN de 6 dígitos.")
                    StepRow("3", "Recibe", "Los archivos llegan cifrados y se guardan en tu móvil.")
                }
            }
            item {
                PrimaryActionButton(
                    text = "Activar recepción",
                    icon = Icons.Rounded.Download,
                    onClick = {
                        haptics.tick()
                        onStart()
                    },
                )
            }
            item {
                SecurityNote()
            }
        } else {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        label = when {
                            transfer is TransferUiState.Receiving -> "Recibiendo"
                            receive.peerName != null -> "Conectado"
                            else -> "Esperando"
                        },
                        active = true,
                    )
                    StatusPill(label = "Cifrado", active = true, color = Primary)
                }
            }

            if (!connection.wifiP2pEnabled) {
                item {
                    HintCard(
                        title = "Wi-Fi desactivado",
                        text = "Activa Wi-Fi para crear el grupo y recibir archivos.",
                        actionLabel = "Activar",
                        onAction = onOpenWifiSettings,
                        tone = Warning,
                        icon = Icons.Rounded.Wifi,
                    )
                }
            }

            item {
                GlassCard(borderColor = Primary.copy(alpha = 0.3f)) {
                    Text(
                        text = "Código de emparejamiento",
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatPin(pin),
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Dilo o muéstralo al emisor. No se envía por la red.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SecondaryActionButton(
                            text = "Copiar",
                            icon = Icons.Rounded.ContentCopy,
                            onClick = {
                                haptics.tick()
                                onCopyPin()
                            },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryActionButton(
                            text = "Nuevo código",
                            icon = Icons.Rounded.Refresh,
                            onClick = {
                                haptics.tick()
                                onNewPin()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (receive.badPinAttempts > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Código incorrecto (${receive.badPinAttempts}/3). Al tercer intento se genera uno nuevo.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Warning,
                        )
                    }
                }
            }

            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Nfc,
                                contentDescription = null,
                                tint = if (nfcAvailable && nfcEnabled) Primary else TextMuted,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Toque NFC",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = when {
                                    !nfcAvailable -> "NFC no disponible en este móvil."
                                    !nfcEnabled -> "NFC desactivado. Actívalo en Ajustes."
                                    nfcReaderActive -> "Acerca un tag NFC para grabar el código."
                                    else -> "El emisor puede leer el código tocando el móvil."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                        if (nfcAvailable && nfcEnabled) {
                            if (nfcReaderActive) {
                                TextButton(onClick = onStopNfc) {
                                    Text("Cancelar", color = TextSecondary)
                                }
                            } else {
                                SecondaryActionButton(
                                    text = "Grabar tag",
                                    onClick = {
                                        haptics.tick()
                                        onNfcWrite()
                                    },
                                )
                            }
                        }
                    }
                    if (nfcAvailable && nfcEnabled && !nfcReaderActive) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "También puedes acercar el móvil del emisor a la parte trasera para emparejar sin escribir el código, si ambos tienen NFC.",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                        )
                    }
                }
            }

            if (receive.waiting &&
                transfer !is TransferUiState.Receiving &&
                transfer !is TransferUiState.Completed &&
                transfer !is TransferUiState.Failed
            ) {
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            RadarAnimation(active = true, modifier = Modifier.size(126.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (receive.peerName != null) {
                                        "Conectado con ${receive.peerName}"
                                    } else {
                                        "Esperando al emisor"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = if (receive.peerName != null) {
                                        "Preparando la transferencia segura…"
                                    } else {
                                        "Mantén esta pantalla abierta y comparte el código."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
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
                                    "Conexión segura",
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
                    }

                    is TransferUiState.Receiving -> ProgressCard(
                        title = "Recibiendo",
                        progress = transfer.progress,
                        onCancel = onCancelTransfer,
                    )

                    is TransferUiState.Completed -> ResultCard(
                        success = true,
                        title = "Recibido",
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
                        title = "No se pudo recibir",
                        text = transfer.message,
                        onDismiss = onDismissStatus,
                    )

                    else -> Unit
                }
            }

            if (receive.receivedFiles.isNotEmpty()) {
                item {
                    SectionHeader(step = "OK", title = "Archivos recibidos")
                }
                item {
                    GlassCard {
                        receive.receivedFiles.take(6).forEach { item ->
                            FileRow(
                                name = item.fileName,
                                size = item.size,
                                mime = com.xyon.airandroid.util.mimeForName(item.fileName),
                                trailing = {
                                    Row {
                                        IconButton(onClick = { onOpenFile(item) }) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.OpenInNew,
                                                contentDescription = "Abrir ${item.fileName}",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                        IconButton(onClick = { onShareFile(item) }) {
                                            Icon(
                                                Icons.Rounded.Share,
                                                contentDescription = "Compartir ${item.fileName}",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        if (receive.receivedFiles.size > 6) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "+${receive.receivedFiles.size - 6} archivos más en Historial",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = {
                        haptics.tick()
                        onStop()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Rounded.StopCircle,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Detener recepción", color = Error)
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: String, title: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(Primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelMedium,
                color = Primary,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun SecurityNote() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Privacidad por diseño",
            style = MaterialTheme.typography.labelLarge,
            color = Primary,
        )
        Text(
            text = "Wi-Fi Direct crea una red privada entre los dos móviles. La transferencia va cifrada con AES-256-GCM y una clave derivada del código de 6 dígitos. Nada sale a internet.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}
