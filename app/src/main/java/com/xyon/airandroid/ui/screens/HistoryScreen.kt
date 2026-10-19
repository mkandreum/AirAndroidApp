package com.xyon.airandroid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xyon.airandroid.model.HistoryItem
import com.xyon.airandroid.model.TransferDirection
import com.xyon.airandroid.model.TransferStatus
import com.xyon.airandroid.ui.components.EmptyState
import com.xyon.airandroid.ui.components.GlassCard
import com.xyon.airandroid.ui.components.ScreenHeader
import com.xyon.airandroid.ui.theme.Border
import com.xyon.airandroid.ui.theme.Error
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.SurfaceCard
import com.xyon.airandroid.ui.theme.TextMuted
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextSecondary
import com.xyon.airandroid.ui.theme.Warning
import com.xyon.airandroid.util.formatBytes
import com.xyon.airandroid.util.formatRelativeTime
import com.xyon.airandroid.util.rememberHaptics

@Composable
fun HistoryScreen(
    history: List<HistoryItem>,
    onClear: () -> Unit,
    onOpenFile: (HistoryItem) -> Unit,
    onShareFile: (HistoryItem) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Historial",
                subtitle = "Tus envíos y recepciones",
                onSettings = {
                    haptics.tick()
                    onOpenSettings()
                },
            )
        }

        item {
            AnimatedVisibility(visible = history.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
                GlassCard {
                    EmptyState(
                        icon = Icons.Rounded.History,
                        title = "Todavía no hay transferencias",
                        text = "Cuando envíes o recibas un archivo aparecerá aquí, con su tamaño y el dispositivo.",
                    )
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (history.size == 1) "1 transferencia" else "${history.size} transferencias",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        haptics.tick()
                        showClearDialog = true
                    }) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("Vaciar", color = TextMuted)
                    }
                }
            }

            items(history, key = { it.id }) { item ->
                HistoryRow(
                    item = item,
                    onOpen = { onOpenFile(item) },
                    onShare = { onShareFile(item) },
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Vaciar historial") },
            text = { Text("Se borrarán los registros, pero los archivos recibidos no se eliminan del móvil.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    haptics.confirm()
                    onClear()
                }) {
                    Text("Vaciar", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
) {
    val haptics = rememberHaptics()
    val receive = item.direction == TransferDirection.RECEIVE
    val accent = if (receive) Primary else TextSecondary
    val statusColor = when (item.status) {
        TransferStatus.COMPLETED -> Primary
        TransferStatus.FAILED -> Error
        TransferStatus.CANCELLED -> Warning
        else -> TextMuted
    }
    val statusText = when (item.status) {
        TransferStatus.COMPLETED -> "Completado"
        TransferStatus.FAILED -> "Fallido"
        TransferStatus.CANCELLED -> "Cancelado"
        TransferStatus.TRANSFERRING -> "En curso"
        TransferStatus.CONNECTING -> "Conectando"
        TransferStatus.WAITING -> "Esperando"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.uri != null) {
                    Modifier.clickable {
                        haptics.tick()
                        onOpen()
                    }
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (receive) {
                        Icons.Rounded.Download
                    } else {
                        Icons.AutoMirrored.Rounded.Send
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${if (receive) "De" else "Para"} ${item.deviceName.ifBlank { "Android" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatBytes(item.size)} · ${formatRelativeTime(item.timestamp)} · $statusText",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
            if (item.uri != null) {
                IconButton(onClick = {
                    haptics.tick()
                    onShare()
                }) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = "Compartir ${item.fileName}",
                        tint = TextMuted,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}
