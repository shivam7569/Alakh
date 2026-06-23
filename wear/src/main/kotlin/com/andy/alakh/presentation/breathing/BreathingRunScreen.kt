package com.andy.alakh.presentation.breathing

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import com.andy.alakh.shared.model.BreathPatternType
import com.andy.alakh.shared.model.BreathingTechnique
import kotlinx.coroutines.delay

private const val POWER_BREATHS = 30          // standard per-round count for ROUNDS techniques
private val Muted = Color(0xFF9AA3A0)

/** Orb states that drive the glow size/brightness. */
private enum class Vis { READY, INHALE, HOLD_TOP, EXHALE, HOLD_BOTTOM }

private fun fmtTime(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)

/** Smoothstep so the breath eases at the top and bottom instead of moving mechanically. */
private fun smoothstep(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

/**
 * Guided run for any catalog technique. The visual is a soft full-screen breathing glow that
 * expands and brightens on the inhale and shrinks/dims on the exhale (no hard edges). Keeps the
 * display on for the whole session and fires distinct haptic cues on every in / out / hold.
 * PACED techniques loop inhale→hold→exhale→hold; ROUNDS (Wim Hof etc.) walk through power-breaths →
 * tap-to-end retention → recovery hold; a no-metronome technique shows a calm pulse.
 */
@Composable
fun BreathingRunScreen(technique: BreathingTechnique, onExit: () -> Unit) {
    val context = LocalContext.current
    val haptics = remember { BreathHaptics(context) }

    // The breathing glow follows the watch's SYSTEM theme color (and updates if the user changes it)
    // — only here; the rest of the app keeps its green accent. Falls back to the app theme primary.
    val glowColor = dynamicColorScheme(context)?.primary ?: MaterialTheme.colorScheme.primary

    // Keep the display on for the whole session (no auto-dim mid-breath); released on exit.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val progress = remember { Animatable(0f) } // 0→1 within the current phase (linear)
    var vis by remember { mutableStateOf(Vis.READY) }
    var label by remember { mutableStateOf("Get ready") }
    var info by remember { mutableStateOf("") }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var cycles by remember { mutableIntStateOf(0) }
    var awaitingTap by remember { mutableStateOf(false) }
    var advanceRequested by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { while (!finished) { delay(1000); elapsedSec++ } }

    suspend fun runPhase(state: Vis, text: String, seconds: Double) {
        vis = state
        label = text
        when (state) {
            Vis.INHALE -> haptics.inhale()
            Vis.EXHALE -> haptics.exhale()
            Vis.HOLD_TOP, Vis.HOLD_BOTTOM -> haptics.hold()
            Vis.READY -> {}
        }
        progress.snapTo(0f)
        if (seconds > 0) progress.animateTo(1f, tween((seconds * 1000).toInt(), easing = LinearEasing))
    }

    LaunchedEffect(Unit) {
        when (technique.patternType) {
            BreathPatternType.ROUNDS -> {
                val rounds = technique.defaultRounds.coerceAtLeast(1)
                val inhale = if (technique.inhaleSec > 0) technique.inhaleSec else 1.6
                val exhale = if (technique.exhaleSec > 0) technique.exhaleSec else 1.6
                for (r in 1..rounds) {
                    info = "Round $r of $rounds"
                    repeat(POWER_BREATHS) {
                        runPhase(Vis.INHALE, "Power breaths", inhale)
                        runPhase(Vis.EXHALE, "Power breaths", exhale)
                    }
                    label = "Exhale & hold"; vis = Vis.HOLD_BOTTOM; progress.snapTo(0f); haptics.exhale()
                    awaitingTap = true
                    advanceRequested = false
                    var held = 0
                    while (!advanceRequested) { delay(1000); held++; info = "Hold ${fmtTime(held)} — tap when you need air" }
                    awaitingTap = false
                    val recovery = if (technique.holdSec > 0) technique.holdSec else 15.0
                    runPhase(Vis.HOLD_TOP, "Breathe in & hold", recovery)
                }
                vis = Vis.READY; label = "Done"; info = "Great work"; finished = true
            }

            else -> {
                if (technique.cycleSec > 0.0) {
                    while (true) {
                        if (technique.inhaleSec > 0) runPhase(Vis.INHALE, "Breathe in", technique.inhaleSec)
                        if (technique.holdSec > 0) runPhase(Vis.HOLD_TOP, "Hold", technique.holdSec)
                        if (technique.exhaleSec > 0) runPhase(Vis.EXHALE, "Breathe out", technique.exhaleSec)
                        if (technique.holdAfterExhaleSec > 0) runPhase(Vis.HOLD_BOTTOM, "Hold", technique.holdAfterExhaleSec)
                        cycles++
                        info = "$cycles cycles"
                    }
                } else {
                    info = technique.nasalNote.ifBlank { "Follow your own pace" }
                    while (true) {
                        runPhase(Vis.INHALE, "Breathe naturally", 4.0)
                        runPhase(Vis.EXHALE, "Breathe naturally", 6.0)
                    }
                }
            }
        }
    }

    // Glow fill 0..1 derived from phase + eased progress.
    val p = progress.value
    val fill = when (vis) {
        Vis.INHALE -> smoothstep(p)
        Vis.HOLD_TOP -> 1f
        Vis.EXHALE -> smoothstep(1f - p)
        Vis.HOLD_BOTTOM, Vis.READY -> 0f
    }

    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            BreathingGlow(modifier = Modifier.fillMaxSize(), fill = fill, color = glowColor)
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
                if (info.isNotBlank()) {
                    Text(info, style = MaterialTheme.typography.bodySmall, color = Color(0xCCFFFFFF), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.weight(1f))
                Text(fmtTime(elapsedSec), style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            when {
                finished -> EdgeButton(onClick = onExit, modifier = Modifier.align(Alignment.BottomCenter)) { Text("Done") }
                awaitingTap -> EdgeButton(onClick = { advanceRequested = true }, modifier = Modifier.align(Alignment.BottomCenter)) { Text("I need to breathe") }
                else -> EdgeButton(onClick = onExit, modifier = Modifier.align(Alignment.BottomCenter)) { Text("End") }
            }
        }
    }
}

/**
 * The breathing glow: a soft, full-screen luminous light that grows and brightens with [fill]
 * (inhale) and shrinks/dims toward the dark screen on the exhale. No hard edges, rings, or arc —
 * a clean, immersive look (inspired by the reference video).
 */
@Composable
private fun BreathingGlow(modifier: Modifier, fill: Float, color: Color) {
    Canvas(modifier = modifier) {
        val f = fill.coerceIn(0f, 1f)
        val cx = size.width / 2f
        val cy = size.height * 0.46f          // bloom sits slightly above center
        val maxDim = size.maxDimension
        val center = Offset(cx, cy)

        // Ambient halo: fills the screen on inhale, fades to the dark edges.
        val ambientR = lerp(maxDim * 0.42f, maxDim * 1.05f, f)
        val ambientA = lerp(0.14f, 0.55f, f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = ambientA), color.copy(alpha = ambientA * 0.35f), Color.Transparent),
                center = center,
                radius = ambientR,
            ),
        )
        // Luminous bloom core for a soft, lit-from-within center (a lighter tint of the glow color).
        val coreR = lerp(maxDim * 0.16f, maxDim * 0.5f, f)
        val coreA = lerp(0.22f, 0.9f, f)
        val bloom = lerpColor(color, Color.White, 0.45f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(bloom.copy(alpha = coreA), color.copy(alpha = coreA * 0.5f), Color.Transparent),
                center = center,
                radius = coreR,
            ),
        )
    }
}

/** Distinct haptic cues for each breathing phase. */
private class BreathHaptics(context: Context) {
    private val vibrator: Vibrator? =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator

    /** Two rising pulses for "in". */
    fun inhale() = play(longArrayOf(0, 35, 55, 90), intArrayOf(0, 110, 0, 200))

    /** One long soft pulse for "out". */
    fun exhale() = play(longArrayOf(0, 180), intArrayOf(0, 110))

    /** A single tiny tick for "hold". */
    fun hold() = play(longArrayOf(0, 28), intArrayOf(0, 90))

    private fun play(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1)) }
    }
}
