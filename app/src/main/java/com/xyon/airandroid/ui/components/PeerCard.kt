package com.xyon.airandroid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xyon.airandroid.model.PeerDevice
import com.xyon.airandroid.ui.theme.Border
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.SurfaceCard
import com.xyon.airandroid.ui.theme.TextMuted
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextSecondary
import com.xyon.airandroid.ui.theme.Warning
import com.xyon.airandroid.util.rememberHaptics

@Composable
fun PeerCard(
    peer: PeerDevice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val available = peer.status == 3 || peer.status == 0
    val borderColor by animateColorAsState(
        targetValue = if (selected) Primary else Border,
        animationSpec = tween(180),
        label = "peerBorder",
    )
    val background by animateColorAsState(
        targetValue = if (selected) Primary.copy(alpha = 0.08f) else SurfaceCard,
        animationSpec = tween(180),
        label = "peerBackground",
    )
    val statusText = when (peer.status) {
        0 -> "Conectado"
        1 -> "Invitado"
        2 -> "Error"
        3 -> "Disponible"
        else -> "No disponible"
    }
    val statusColor = when (peer.status) {
        0, 3 -> Primary
        1 -> Warning
        else -> TextMuted
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = available) {
                haptics.tick()
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = background,
        border = BorderStroke(1.dp, borderColor),
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
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = if (available) Primary else TextMuted,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = peer.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$statusText · ${peer.address.takeLast(8)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = Primary,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Spacer(Modifier.width(22.dp))
            }
        }
    }
}
