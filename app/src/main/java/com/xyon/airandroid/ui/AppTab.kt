package com.xyon.airandroid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    SEND("Enviar", Icons.AutoMirrored.Rounded.Send),
    RECEIVE("Recibir", Icons.Rounded.Download),
    HISTORY("Historial", Icons.Rounded.History),
}
