package com.andy.alakh.presentation.workout

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import com.andy.alakh.health.PermissionHelper
import com.andy.alakh.health.UserProfile
import com.andy.alakh.health.WorkoutSensors
import com.andy.alakh.shared.rules.HealthRules
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val Muted = Color(0xFF9AA3A0)
private val ZoneText = Color(0xFFE6E6EE)

// The four heart-rate zones shown on the gauge (cool → warm), each its own ring segment.
private val ZoneColors = listOf(Color(0xFF36D0C8), Color(0xFF46C66E), Color(0xFFE8A93D), Color(0xFFE0573E))
private val ZoneNames = listOf("Light", "Fat burn", "Cardio", "Peak")

/** Which of the 4 gauge zones the value sits in (0–3), from the gauge fraction. */
private fun gaugeZone(frac: Float): Int = (frac * 4f).toInt().coerceIn(0, 3)

private fun fmtTime(elapsedMs: Long): String {
    val total = (elapsedMs / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Live workout monitor, matched to the watch's native HR screen: a ring of FOUR zone segments
 * (lavender track) that fill up to a white dot in the CURRENT zone's color, with a heart in the
 * center that takes the same zone color, plus the BPM, zone name, and labeled calories + time.
 */
@Composable
fun WorkoutMonitorScreen() {
    val vm: WorkoutViewModel = viewModel()
    val metrics by vm.metrics.collectAsStateWithLifecycle()
    val diagnostic by vm.diagnostic.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val maxHr = remember { UserProfile.maxHeartRate(context) }
    val track = dynamicColorScheme(context)?.primary ?: MaterialTheme.colorScheme.primary // lavender system theme

    var bodySensorsMissing by remember { mutableStateOf(PermissionHelper.missingBodySensors(context)) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        bodySensorsMissing = PermissionHelper.missingBodySensors(context)
        WorkoutSensors.start(context)
    }

    val hr = metrics.heartRateBpm
    val pct = hr?.let { HealthRules.heartRatePercent(it, maxHr) } ?: 0
    // Gauge spans ~40%→100% of max HR (≈ Light / Fat burn / Cardio / Peak), so resting sits near the start.
    val frac = if (hr != null) ((pct - 40f) / 60f).coerceIn(0f, 1f) else 0f
    val zone = gaugeZone(frac)
    val zoneColor = if (hr != null) ZoneColors[zone] else Muted

    val subline = when {
        hr != null -> "${ZoneNames[zone]} zone"
        bodySensorsMissing -> "Body Sensors needed"
        else -> diagnostic ?: "Waiting for heart rate…"
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Box(contentAlignment = Alignment.Center) {
                ZoneGauge(frac = frac, active = zoneColor, track = track, hasValue = hr != null, modifier = Modifier.size(76.dp))
            }
            Text("Heart rate", fontSize = 13.sp, color = track)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(hr?.toString() ?: "—", fontSize = 46.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(" bpm", fontSize = 16.sp, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(
                subline,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp,
                color = ZoneText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                StatMini("${metrics.calories?.toInt() ?: 0}", "kcal")
                StatMini(fmtTime(metrics.elapsedMs), "time")
            }
            if (bodySensorsMissing) {
                Button(onClick = { permLauncher.launch(PermissionHelper.SENSOR_PERMISSIONS.toTypedArray()) }) {
                    Text("Grant sensors", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StatMini(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(label, fontSize = 9.sp, color = Muted)
    }
}

/**
 * The native-style HR gauge: a ~320° ring split into FOUR zone segments (gaps between). Each segment
 * is dim [track] where the value hasn't reached, and filled in the current zone's [active] color up
 * to the white dot. The outline heart in the center takes the same [active] color. No overlapping
 * layers — filled and unfilled portions are distinct, non-overlapping arc ranges.
 */
@Composable
private fun ZoneGauge(frac: Float, active: Color, track: Color, hasValue: Boolean, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 6.dp.toPx()
        val r = (min(size.width, size.height) - stroke) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val topLeft = Offset(cx - r, cy - r)
        val arcSize = Size(r * 2f, r * 2f)

        val startDeg = 110f
        val totalDeg = 320f
        val gap = 8f
        val seg = (totalDeg - 3f * gap) / 4f
        val dotDeg = startDeg + totalDeg * frac.coerceIn(0f, 1f)
        val dim = track.copy(alpha = 0.5f)

        for (i in 0 until 4) {
            val s0 = startDeg + i * (seg + gap)
            val s1 = s0 + seg
            if (hasValue && dotDeg > s0) { // filled portion of this segment
                val end = min(s1, dotDeg)
                drawArc(active, s0, end - s0, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            if (!hasValue || dotDeg < s1) { // unfilled (dim) portion
                val st = max(s0, dotDeg)
                drawArc(dim, st, s1 - st, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }

        if (hasValue) {
            val ang = dotDeg * (PI / 180.0)
            val dot = Offset(cx + r * cos(ang).toFloat(), cy + r * sin(ang).toFloat())
            drawCircle(Color.White, stroke * 0.82f, dot)
            drawCircle(active, stroke * 0.42f, dot)
        }

        // Outline heart in the center, in the current zone color.
        val hs = r * 0.5f
        val heart = Path().apply {
            moveTo(cx, cy - hs * 0.25f)
            cubicTo(cx - hs * 0.5f, cy - hs * 0.95f, cx - hs * 1.3f, cy - hs * 0.1f, cx, cy + hs * 0.7f)
            cubicTo(cx + hs * 1.3f, cy - hs * 0.1f, cx + hs * 0.5f, cy - hs * 0.95f, cx, cy - hs * 0.25f)
        }
        drawPath(heart, active, style = Stroke(width = stroke * 0.62f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
