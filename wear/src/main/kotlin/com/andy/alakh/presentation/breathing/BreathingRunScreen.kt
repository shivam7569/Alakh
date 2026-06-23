package com.andy.alakh.presentation.breathing

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.theme.AlakhAccent
import com.andy.alakh.shared.model.BreathPatternType
import com.andy.alakh.shared.model.BreathingTechnique
import kotlinx.coroutines.delay
import kotlin.math.min

private const val POWER_BREATHS = 30          // standard per-round count for ROUNDS techniques
private val Muted = Color(0xFF9AA3A0)

/** Orb states that drive the animation. The arc fills over a phase; the orb is full on holds. */
private enum class Vis { READY, INHALE, HOLD_TOP, EXHALE, HOLD_BOTTOM }

private fun fmtTime(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)

/** Smoothstep so the breath eases at the top and bottom instead of moving mechanically. */
private fun smoothstep(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

/**
 * Guided run for any catalog technique with an animated breathing orb (glow + concentric rings +
 * a phase-progress arc), keep-screen-on, and haptic cues on every in / out / hold transition.
 * PACED techniques loop inhale→hold→exhale→hold; FREEFORM-with-timing animates a gentle/rapid
 * paced breath; a no-metronome technique shows a calm pulse; ROUNDS (Wim Hof etc.) walk through
 * power-breaths → tap-to-end retention → recovery hold per round.
 */
@Composable
fun BreathingRunScreen(technique: BreathingTechnique, onExit: () -> Unit) {
    val context = LocalContext.current
    val haptics = remember { BreathHaptics(context) }

    // 1) Keep the display on for the whole session (no auto-dim mid-breath).
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

    // Elapsed clock (delay-based, no wall-clock reads).
    LaunchedEffect(Unit) { while (!finished) { delay(1000); elapsedSec++ } }

    // One phase: set visuals + fire the haptic, then animate progress over its duration.
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
                    // Retention: exhale and hold until the user taps.
                    label = "Exhale & hold"; vis = Vis.HOLD_BOTTOM; progress.snapTo(0f); haptics.exhale()
                    awaitingTap = true
                    advanceRequested = false
                    var held = 0
                    while (!advanceRequested) { delay(1000); held++; info = "Hold ${fmtTime(held)} — tap when you need air" }
                    awaitingTap = false
                    // Recovery: big breath in, hold.
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
                    // No metronome (e.g. breath counting): a calm pulse, gentle in-cue only.
                    info = technique.nasalNote.ifBlank { "Follow your own pace" }
                    while (true) {
                        runPhase(Vis.INHALE, "Breathe naturally", 4.0)
                        runPhase(Vis.EXHALE, "Breathe naturally", 6.0)
                    }
                }
            }
        }
    }

    // Derive orb fill (0..1) and the arc fraction from the phase + progress.
    val p = progress.value
    val orbFrac = when (vis) {
        Vis.INHALE -> smoothstep(p)
        Vis.HOLD_TOP -> 1f
        Vis.EXHALE -> smoothstep(1f - p)
        Vis.HOLD_BOTTOM, Vis.READY -> 0f
    }
    val arc = if (vis == Vis.HOLD_BOTTOM && awaitingTap) 0f else p

    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 54.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = AlakhAccent, textAlign = TextAlign.Center)
                if (info.isNotBlank()) {
                    Text(info, style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center)
                }
                BreathingOrb(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    fill = orbFrac,
                    arc = arc,
                    color = AlakhAccent,
                )
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

/** The animated orb: a soft glow halo, faint concentric rings, a gradient sphere, and a progress arc. */
@Composable
private fun BreathingOrb(modifier: Modifier, fill: Float, arc: Float, color: Color) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(size.width, size.height) * 0.40f
        val minR = maxR * 0.42f
        val r = lerp(minR, maxR, fill.coerceIn(0f, 1f))
        val center = Offset(cx, cy)

        // Soft glow halo behind the orb.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                center = center,
                radius = r * 1.9f,
            ),
            radius = r * 1.9f,
            center = center,
        )
        // Faint concentric rings for depth.
        for (i in 1..3) {
            drawCircle(
                color = color.copy(alpha = 0.05f * (4 - i)),
                radius = r * (1f + 0.16f * i),
                center = center,
                style = Stroke(width = 1.5f),
            )
        }
        // The orb itself, lit from upper-left for a spherical feel.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.55f)),
                center = Offset(cx - r * 0.25f, cy - r * 0.25f),
                radius = r * 1.25f,
            ),
            radius = r,
            center = center,
        )
        // Phase-progress arc on a fixed ring around the orb's max extent.
        val arcR = maxR * 1.12f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * arc.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(cx - arcR, cy - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 4f),
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
