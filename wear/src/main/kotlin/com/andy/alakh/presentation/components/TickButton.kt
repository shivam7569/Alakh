package com.andy.alakh.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val Accent = Color(0xFF34C796)

/** Circular accent confirm button with a drawn checkmark + haptic — a tap-to-tick action. */
@Composable
fun TickButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 60.dp,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Accent)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter * 0.5f)) {
            val w = size.width
            val h = size.height
            val check = Path().apply {
                moveTo(w * 0.16f, h * 0.55f)
                lineTo(w * 0.42f, h * 0.80f)
                lineTo(w * 0.86f, h * 0.24f)
            }
            drawPath(
                path = check,
                color = Color.White,
                style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
