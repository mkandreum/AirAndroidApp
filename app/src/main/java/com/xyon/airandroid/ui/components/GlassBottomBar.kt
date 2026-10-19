package com.xyon.airandroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.xyon.airandroid.ui.AppTab
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.TextMuted
import com.xyon.airandroid.util.rememberHaptics

@Composable
fun GlassBottomBar(
    current: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 520f,
                    ),
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0D0D0D))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = tab == current,
                    onClick = { onSelect(tab) },
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    val background by animateColorAsState(
        targetValue = if (selected) Primary.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(200),
        label = "pillBg_${tab.name}",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Primary else TextMuted,
        animationSpec = tween(200),
        label = "pillFg_${tab.name}",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 620f),
        label = "pillIcon_${tab.name}",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .selectable(
                selected = selected,
                onClick = {
                    haptics.tick()
                    onClick()
                },
                role = Role.Tab,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(160)) + expandHorizontally(tween(180)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(160)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                )
            }
        }
    }
}
