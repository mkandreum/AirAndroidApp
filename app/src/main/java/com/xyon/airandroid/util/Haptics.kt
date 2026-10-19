package com.xyon.airandroid.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

class Haptics(
    private val view: View,
    private val feedback: HapticFeedback,
) {
    fun tick() {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun reject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val feedback = LocalHapticFeedback.current
    return remember(view, feedback) { Haptics(view, feedback) }
}
