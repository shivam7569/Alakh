package com.andy.alakh.presentation.breathing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.theme.AlakhAccent
import com.andy.alakh.shared.model.BreathPatternType
import com.andy.alakh.shared.model.BreathingTechnique
import kotlinx.coroutines.delay

private const val MIN_SCALE = 0.45f
private const val MAX_SCALE = 1.0f
private const val POWER_BREATHS = 30          // standard per-round count for ROUNDS techniques
private val Muted = androidx.compose.ui.graphics.Color(0xFF9AA3A0)

private fun fmtTime(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)

/**
 * Guided run for any catalog technique. PACED techniques animate inhale→hold→exhale→hold on a loop;
 * FREEFORM-with-timing animates a gentle/rapid paced breath; a no-metronome technique (e.g. breath
 * counting) shows a calm "breathe naturally" pulse; ROUNDS techniques (Wim Hof etc.) walk through
 * power-breaths → tap-to-end retention → recovery hold for each round.
 */
@Composable
fun BreathingRunScreen(technique: BreathingTechnique, onExit: () -> Unit) {
    val scale = remember { Animatable(MIN_SCALE) }
    var phase by remember { mutableStateOf("Get ready") }
    var info by remember { mutableStateOf("") }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var cycles by remember { mutableIntStateOf(0) }
    var awaitingTap by remember { mutableStateOf(false) }
    var advanceRequested by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    // Elapsed clock (uses delay, no wall-clock reads).
    LaunchedEffect(Unit) {
        while (!finished) { delay(1000); elapsedSec++ }
    }

    LaunchedEffect(Unit) {
        when (technique.patternType) {
            BreathPatternType.ROUNDS -> {
                val rounds = technique.defaultRounds.coerceAtLeast(1)
                val inhaleMs = ((if (technique.inhaleSec > 0) technique.inhaleSec else 1.6) * 1000).toInt()
                val exhaleMs = ((if (technique.exhaleSec > 0) technique.exhaleSec else 1.6) * 1000).toInt()
                for (r in 1..rounds) {
                    info = "Round $r of $rounds"
                    phase = "Power breaths"
                    repeat(POWER_BREATHS) {
                        scale.animateTo(MAX_SCALE, tween(inhaleMs, easing = LinearEasing))
                        scale.animateTo(MIN_SCALE, tween(exhaleMs, easing = LinearEasing))
                    }
                    // Retention: exhale and hold until the user needs to breathe.
                    phase = "Exhale & hold"
                    scale.animateTo(MIN_SCALE, tween(700))
                    awaitingTap = true
                    advanceRequested = false
                    var held = 0
                    while (!advanceRequested) { delay(1000); held++; info = "Hold ${fmtTime(held)} — tap when you need air" }
                    awaitingTap = false
                    // Recovery: big breath in, hold.
                    phase = "Breathe in & hold"
                    scale.animateTo(MAX_SCALE, tween(1500))
                    val recoveryMs = ((if (technique.holdSec > 0) technique.holdSec else 15.0) * 1000).toLong()
                    delay(recoveryMs)
                }
                phase = "Done"
                info = "Great work"
                finished = true
            }

            else -> {
                if (technique.cycleSec > 0.0) {
                    while (true) {
                        if (technique.inhaleSec > 0) {
                            phase = "Breathe in"
                            scale.animateTo(MAX_SCALE, tween((technique.inhaleSec * 1000).toInt(), easing = LinearEasing))
                        }
                        if (technique.holdSec > 0) { phase = "Hold"; delay((technique.holdSec * 1000).toLong()) }
                        if (technique.exhaleSec > 0) {
                            phase = "Breathe out"
                            scale.animateTo(MIN_SCALE, tween((technique.exhaleSec * 1000).toInt(), easing = LinearEasing))
                        }
                        if (technique.holdAfterExhaleSec > 0) { phase = "Hold"; delay((technique.holdAfterExhaleSec * 1000).toLong()) }
                        cycles++
                        info = "$cycles cycles"
                    }
                } else {
                    phase = "Breathe naturally"
                    info = technique.nasalNote.ifBlank { "Follow your own pace" }
                    while (true) {
                        scale.animateTo(MAX_SCALE, tween(4000, easing = LinearEasing))
                        scale.animateTo(MIN_SCALE, tween(6000, easing = LinearEasing))
                    }
                }
            }
        }
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(phase, style = MaterialTheme.typography.titleMedium, color = AlakhAccent, textAlign = TextAlign.Center)
            if (info.isNotBlank()) {
                Text(info, style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center)
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(scale.value)
                    .background(AlakhAccent, CircleShape),
            )
            Text(fmtTime(elapsedSec), style = MaterialTheme.typography.bodySmall, color = Muted)
            Spacer(Modifier.height(2.dp))
            when {
                finished -> Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                awaitingTap -> Button(onClick = { advanceRequested = true }, modifier = Modifier.fillMaxWidth()) { Text("I need to breathe") }
                else -> Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("End") }
            }
        }
    }
}
