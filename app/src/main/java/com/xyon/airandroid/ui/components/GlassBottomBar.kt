package com.xyon.airandroid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.xyon.airandroid.ui.theme.Border
import com.xyon.airandroid.ui.theme.Glass
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.TextSecondary
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
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(33.dp))
                .background(Glass)
                .border(1.dp, Border, RoundedCornerShape(33.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AppTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = tab == current,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
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
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val background by animateColorAsState(
        targetValue = if (selected) Primary.copy(alpha = 0.13f) else Color.Transparent,
        animationSpec = tween(200),
        label = "tabBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Primary else TextSecondary,
        animationSpec = tween(200),
        label = "tabContent",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "tabScale",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(27.dp))
            .background(background)
            .selectable(
                selected = selected,
                onClick = {
                    haptics.tick()
                    onClick()
                },
                role = Role.Tab,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            )
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}
