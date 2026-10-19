package com.xyon.airandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xyon.airandroid.BuildConfig
import com.xyon.airandroid.model.AppSettings
import com.xyon.airandroid.ui.theme.BorderStrong
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.SurfaceCard
import com.xyon.airandroid.ui.theme.SurfaceElevated
import com.xyon.airandroid.ui.theme.TextMuted
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextSecondary
import com.xyon.airandroid.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
    onSaveName: (String) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    diagnosticsAvailable: Boolean,
    onShareDiagnostics: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(settings.deviceName) { mutableStateOf(settings.deviceName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 22.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { value -> name = value.take(32) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre del dispositivo") },
                supportingText = {
                    Text("Se usa en AirAndroid y en el mensaje de emparejamiento.")
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderStrong,
                    focusedLabelColor = Primary,
                    cursorColor = Primary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )

            PrimaryActionButton(
                text = "Guardar nombre",
                onClick = {
                    onSaveName(name.trim())
                    onDismiss()
                },
                enabled = name.isNotBlank(),
            )

            HorizontalDivider(color = BorderStrong)

            Text(
                text = "Seguridad",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            SecurityRow(
                icon = { Icon(Icons.Rounded.Lock, null, tint = Primary) },
                title = "Cifrado AES-256-GCM",
                subtitle = "Cada bloque viaja cifrado y autenticado.",
            )
            SecurityRow(
                icon = { Icon(Icons.Rounded.Key, null, tint = Primary) },
                title = "PIN de 6 dígitos",
                subtitle = "El receptor lo muestra y el emisor lo introduce.",
            )
            SecurityRow(
                icon = { Icon(Icons.Rounded.Wifi, null, tint = Primary) },
                title = "Wi-Fi Direct, sin internet",
                subtitle = "Los datos no pasan por ningún servidor.",
            )

            HorizontalDivider(color = BorderStrong)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (nfcAvailable && nfcEnabled) {
                                Primary.copy(alpha = 0.1f)
                            } else {
                                SurfaceElevated
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Nfc,
                        contentDescription = null,
                        tint = if (nfcAvailable && nfcEnabled) Primary else TextMuted,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("NFC", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = when {
                            !nfcAvailable -> "No disponible en este móvil"
                            nfcEnabled -> "Listo para tocar y emparejar"
                            else -> "Desactivado"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (nfcAvailable && !nfcEnabled) Warning else TextSecondary,
                    )
                }
                if (nfcAvailable && !nfcEnabled) {
                    SecondaryActionButton(
                        text = "Activar",
                        onClick = onOpenNfcSettings,
                    )
                }
            }

            SecondaryActionButton(
                text = "Ajustes de Wi-Fi",
                onClick = onOpenWifiSettings,
                icon = Icons.Rounded.Wifi,
                modifier = Modifier.fillMaxWidth(),
            )

            if (diagnosticsAvailable) {
                SecondaryActionButton(
                    text = "Compartir último error",
                    onClick = onShareDiagnostics,
                    icon = Icons.Rounded.BugReport,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "AirAndroid ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SecurityRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
