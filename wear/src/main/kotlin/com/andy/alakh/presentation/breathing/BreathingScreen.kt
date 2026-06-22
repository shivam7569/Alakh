package com.andy.alakh.presentation.breathing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.andy.alakh.shared.rules.HealthRules
import kotlinx.coroutines.delay

/**
 * Guided breathing. Fully self-contained (no sensors / permissions): a circle
 * expands on inhale and contracts on exhale, following the shared box-breathing
 * pattern. This feature works out of the box.
 */
@Composable
fun BreathingScreen() {
    val pattern = HealthRules.BOX_BREATHING
    val scale = remember { Animatable(MIN_SCALE) }
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("Tap to begin") }

    LaunchedEffect(running) {
        if (!running) {
            scale.snapTo(MIN_SCALE)
            phase = "Tap to begin"
            return@LaunchedEffect
        }
        while (true) {
            phase = "Breathe in"
            scale.animateTo(MAX_SCALE, tween(pattern.inhaleSec * 1000, easing = LinearEasing))
            if (pattern.holdSec > 0) { phase = "Hold"; delay(pattern.holdSec * 1000L) }
            phase = "Breathe out"
            scale.animateTo(MIN_SCALE, tween(pattern.exhaleSec * 1000, easing = LinearEasing))
            if (pattern.holdAfterExhaleSec > 0) { phase = "Hold"; delay(pattern.holdAfterExhaleSec * 1000L) }
        }
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(phase)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale.value)
                    .background(Color(0xFF22B8CF), CircleShape),
            )
            Button(onClick = { running = !running }) {
                Text(if (running) "Stop" else "Start")
            }
        }
    }
}

private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 1.0f

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun BreathingScreenPreview() {
    BreathingScreen()
}
