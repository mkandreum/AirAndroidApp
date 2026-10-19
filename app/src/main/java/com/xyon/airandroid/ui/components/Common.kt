package com.xyon.airandroid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xyon.airandroid.ui.theme.Border
import com.xyon.airandroid.ui.theme.BorderStrong
import com.xyon.airandroid.ui.theme.Error
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.SurfaceCard
import com.xyon.airandroid.ui.theme.SurfaceElevated
import com.xyon.airandroid.ui.theme.TextMuted
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextSecondary
import com.xyon.airandroid.ui.theme.Warning
import com.xyon.airandroid.util.formatBytes
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(SurfaceElevated),
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Ajustes",
                tint = TextSecondary,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
fun HeroCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Bolt,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.24f)),
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.17f),
                        Color.Transparent,
                    ),
                ),
            ),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    borderColor: Color = Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = SurfaceCard.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(
    step: String,
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelLarge,
            color = Primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
fun StatusPill(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Primary,
) {
    val dot by animateColorAsState(
        targetValue = if (active) color else TextMuted,
        animationSpec = tween(220),
        label = "pillDot",
    )
    val background by animateColorAsState(
        targetValue = if (active) color.copy(alpha = 0.12f) else SurfaceElevated,
        animationSpec = tween(220),
        label = "pillBackground",
    )
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, dot.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) TextPrimary else TextSecondary,
        )
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "primaryScale",
    )
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.Black,
            disabledContainerColor = SurfaceElevated,
            disabledContentColor = TextMuted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.Black,
            )
        } else {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextPrimary,
            disabledContentColor = TextMuted,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ResultCard(
    success: Boolean,
    title: String,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (success) Primary else Error
    GlassCard(modifier = modifier, borderColor = color.copy(alpha = 0.35f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (success) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.ErrorOutline
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(23.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextSecondary)
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(26.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun HintCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    tone: Color = Warning,
    icon: ImageVector = Icons.Rounded.WifiOff,
) {
    GlassCard(modifier = modifier, borderColor = tone.copy(alpha = 0.35f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            if (actionLabel != null && onAction != null) {
                SecondaryActionButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun FileTypeIcon(
    mime: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val icon = when {
        mime.startsWith("image/") -> Icons.Rounded.Image
        mime.startsWith("video/") -> Icons.Rounded.Movie
        mime.startsWith("audio/") -> Icons.Rounded.Audiotrack
        mime.contains("pdf") -> Icons.Rounded.PictureAsPdf
        mime.contains("zip") || mime.contains("compressed") -> Icons.Rounded.FolderZip
        mime.startsWith("text/") || mime.contains("document") -> Icons.Rounded.Description
        mime.contains("android") -> Icons.Rounded.Android
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3f).dp))
            .background(Primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

@Composable
fun FileRow(
    name: String,
    size: Long,
    mime: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FileTypeIcon(mime)
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatBytes(size),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun RemoveFileButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Quitar archivo",
            tint = TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun RadarAnimation(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!active) {
        Canvas(modifier) { drawStaticRings() }
        return
    }
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarAngle",
    )
    Canvas(modifier) {
        drawStaticRings()
        drawArc(
            color = Primary.copy(alpha = 0.32f),
            startAngle = angle,
            sweepAngle = 58f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
        val radians = Math.toRadians(angle.toDouble())
        val radius = size.minDimension * 0.38f
        val dot = Offset(
            x = center.x + cos(radians).toFloat() * radius,
            y = center.y + sin(radians).toFloat() * radius,
        )
        drawCircle(color = Primary, radius = 4.dp.toPx(), center = dot)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStaticRings() {
    drawCircle(color = Primary.copy(alpha = 0.06f), radius = size.minDimension * 0.48f)
    drawCircle(
        color = Primary.copy(alpha = 0.14f),
        radius = size.minDimension * 0.36f,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawCircle(
        color = Primary.copy(alpha = 0.1f),
        radius = size.minDimension * 0.22f,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawCircle(
        color = Primary.copy(alpha = 0.22f),
        radius = size.minDimension * 0.42f,
        style = Stroke(width = 1.dp.toPx()),
    )
}
