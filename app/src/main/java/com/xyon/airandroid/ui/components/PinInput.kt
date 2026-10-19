package com.xyon.airandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xyon.airandroid.ui.theme.Border
import com.xyon.airandroid.ui.theme.Primary
import com.xyon.airandroid.ui.theme.PrimaryMuted
import com.xyon.airandroid.ui.theme.SurfaceElevated
import com.xyon.airandroid.ui.theme.TextPrimary
import com.xyon.airandroid.ui.theme.TextMuted

@Composable
fun PinInput(
    pin: String,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(6) { index ->
                val char = pin.getOrNull(index)
                val active = focused && enabled && index == pin.length.coerceAtMost(5)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.82f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (char != null) PrimaryMuted else SurfaceElevated)
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) Primary else Border,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = char?.toString().orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (char != null) TextPrimary else TextMuted,
                    )
                }
            }
        }

        BasicTextField(
            value = pin,
            onValueChange = { value ->
                onPinChange(value.filter(Char::isDigit).take(6))
            },
            enabled = enabled,
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(
                color = Color.Transparent,
                fontSize = 1.sp,
            ),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { keyboard?.hide() },
            ),
        )
    }
}
