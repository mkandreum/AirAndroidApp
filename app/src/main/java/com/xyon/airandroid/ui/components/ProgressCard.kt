package com.xyon.airandroid.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xyon.airandroid.model.TransferProgress
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.SurfaceElevated
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextSecondary
import com.xyon.airandroid.util.formatBytes
import com.xyon.airandroid.util.formatSpeed

@Composable
fun ProgressCard(
    title: String,
    progress: TransferProgress,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(160),
        label = "transferProgress",
    )
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary,
                )
                Text(
                    text = progress.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${(animated * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        }

        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = Primary,
            trackColor = SurfaceElevated,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "${formatBytes(progress.bytes)} de ${formatBytes(progress.total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = formatSpeed(progress.speedBytesPerSecond),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            TextButton(onClick = onCancel) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary,
                )
                Spacer(Modifier.width(6.dp))
                Text("Cancelar", color = TextSecondary)
            }
        }
    }
}
