package com.andy.alakh.presentation.breathing

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import com.andy.alakh.shared.model.BreathPatternType
import com.andy.alakh.shared.model.BreathingTechnique
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val POWER_BREATHS = 30          // standard per-round count for ROUNDS techniques
private val Muted = Color(0xFF9AA3A0)
private val TWO_PI = (2.0 * PI).toFloat()

/** Phases that drive the breathing (the ring radius grows on inhale, shrinks on exhale). */
private enum class Vis { READY, INHALE, HOLD_TOP, EXHALE, HOLD_BOTTOM }

private fun fmtTime(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)

/** Smoothstep so the breath eases at the top and bottom instead of moving mechanically. */
private fun smoothstep(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

/**
 * Guided run for any catalog technique. The whole screen is the animation — an "arc sliver" comet
 * riding a slowly rotating, mostly-circular ring whose radius breathes (grows on inhale, shrinks on
 * exhale). There's no persistent chrome: a tap reveals an End popup (tap outside it to resume); during
 * a ROUNDS retention hold a tap advances instead. Keeps the display on and gives haptic in/out/hold cues.
 */
@Composable
fun BreathingRunScreen(technique: BreathingTechnique, onExit: () -> Unit) {
    val context = LocalContext.current
    val haptics = remember { BreathHaptics(context) }

    // The breathing visual follows the watch SYSTEM theme color (only here); the rest stays green.
    val ringColor = dynamicColorScheme(context)?.primary ?: MaterialTheme.colorScheme.primary

    // Keep the display on for the whole session (no auto-dim mid-breath); released on exit.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Steady ambient motion: the ring slowly precesses, the comet laps the ring.
    val spin = rememberInfiniteTransition(label = "breath-spin")
    val ringRot by spin.animateFloat(
        initialValue = 0f, targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing)),
        label = "ringRot",
    )
    val cometProg by spin.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "cometProg",
    )
    val cometAngle = (-PI / 2.0).toFloat() + cometProg * TWO_PI

    val progress = remember { Animatable(0f) } // 0→1 within the current phase (linear)
    var vis by remember { mutableStateOf(Vis.READY) }
    var label by remember { mutableStateOf("Get ready") }
    var info by remember { mutableStateOf("") }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var cycles by remember { mutableIntStateOf(0) }
    var awaitingTap by remember { mutableStateOf(false) }
    var advanceRequested by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { while (!finished) { delay(1000); elapsedSec++ } }

    // Auto-hide the End popup after a few seconds so the user can just keep breathing.
    LaunchedEffect(controlsVisible) { if (controlsVisible) { delay(4000); controlsVisible = false } }

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

    // Ring radius fraction 0..1 derived from phase + eased progress.
    val p = progress.value
    val fill = when (vis) {
        Vis.INHALE -> smoothstep(p)
        Vis.HOLD_TOP -> 1f
        Vis.EXHALE -> smoothstep(1f - p)
        Vis.HOLD_BOTTOM, Vis.READY -> 0f
    }

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(awaitingTap, finished) {
                    detectTapGestures {
                        when {
                            finished -> onExit()
                            awaitingTap -> advanceRequested = true   // retention: tap = "I need to breathe"
                            else -> controlsVisible = !controlsVisible
                        }
                    }
                },
        ) {
            BreathingRingComet(
                modifier = Modifier.fillMaxSize(),
                fill = fill,
                ringRot = ringRot,
                cometAngle = cometAngle,
                color = ringColor,
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
                if (info.isNotBlank()) {
                    Text(info, style = MaterialTheme.typography.bodySmall, color = Color(0xCCFFFFFF), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.weight(1f))
                Text(fmtTime(elapsedSec), style = MaterialTheme.typography.bodySmall, color = Muted)
            }

            // Tap-to-reveal End popup (tap outside it to resume). Auto-hides after a few seconds.
            if (controlsVisible && !finished) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xB3000000))
                        .pointerInput(Unit) { detectTapGestures { controlsVisible = false } },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = onExit, modifier = Modifier.padding(horizontal = 28.dp)) { Text("End") }
                        Text("tap to resume", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            // When a ROUNDS session completes, surface a Done action.
            if (finished) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xB3000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(onClick = onExit, modifier = Modifier.padding(horizontal = 28.dp)) { Text("Done") }
                }
            }
        }
    }
}

/**
 * An "arc sliver" comet riding a slowly rotating, mostly-circular ring (Ry = 0.9·Rx). The ring's
 * radius breathes with [fill]; [ringRot] precesses the (slightly elliptical) ring; the comet sits at
 * [cometAngle] with a short faded dot trail behind it. Flat palette (no soft glow gradients).
 */
@Composable
private fun BreathingRingComet(modifier: Modifier, fill: Float, ringRot: Float, cometAngle: Float, color: Color) {
    val headColor = lerpColor(color, Color.White, 0.55f)
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val minDim = min(size.width, size.height)
        val rx = lerp(minDim * 0.21f, minDim * 0.36f, fill.coerceIn(0f, 1f))
        val ry = rx * 0.90f
        val cosR = cos(ringRot)
        val sinR = sin(ringRot)
        fun pointAt(a: Float): Offset {
            val ex = rx * cos(a)
            val ey = ry * sin(a)
            return Offset(cx + ex * cosR - ey * sinR, cy + ex * sinR + ey * cosR)
        }

        // Ring outline (thin, faint, flat).
        val ring = Path()
        val seg = 72
        for (i in 0..seg) {
            val pt = pointAt(i / seg.toFloat() * TWO_PI)
            if (i == 0) ring.moveTo(pt.x, pt.y) else ring.lineTo(pt.x, pt.y)
        }
        ring.close()
        drawPath(ring, color = color.copy(alpha = 0.18f), style = Stroke(width = 2.5f.dp.toPx()))

        // Fading dot trail behind the comet.
        val trailN = 12
        for (i in trailN downTo 1) {
            val tt = i / trailN.toFloat()
            val pt = pointAt(cometAngle - tt * 0.95f)
            drawCircle(color = color.copy(alpha = 0.32f * (1f - tt)), radius = lerp(1.2f, 3.0f, 1f - tt).dp.toPx(), center = pt)
        }

        // The arc-sliver comet head: a short thick arc following the ring.
        val head = Path()
        val hn = 12
        for (i in 0..hn) {
            val a = cometAngle - 0.30f + 0.30f * (i / hn.toFloat())
            val pt = pointAt(a)
            if (i == 0) head.moveTo(pt.x, pt.y) else head.lineTo(pt.x, pt.y)
        }
        drawPath(head, color = headColor, style = Stroke(width = 5f.dp.toPx(), cap = StrokeCap.Round))
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
