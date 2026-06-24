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
import androidx.compose.ui.graphics.PathEffect
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
import com.andy.alakh.shared.rules.HealthRules.HeartRateZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val Muted = Color(0xFF9AA3A0)
private val Teal = Color(0xFF36D0C8)        // the native HR fill / heart color
private val ZoneText = Color(0xFFE6E6EE)

private fun zoneName(zone: HeartRateZone): String = when (zone) {
    HeartRateZone.REST -> "Resting"
    HeartRateZone.ZONE_1 -> "Warm-up zone"
    HeartRateZone.ZONE_2 -> "Light zone"
    HeartRateZone.ZONE_3 -> "Cardio zone"
    HeartRateZone.ZONE_4 -> "Hard zone"
    HeartRateZone.ZONE_5 -> "Peak zone"
}

private fun fmtTime(elapsedMs: Long): String {
    val total = (elapsedMs / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Live workout monitor, matching the watch's native heart-rate screen: a wide (~320°) gauge with a
 * lavender track, a teal fill + white dot marking the value, a teal heart in the center, the
 * "Heart rate" label, the big BPM, the zone name, and labeled calories + time.
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
    val zone = hr?.let { HealthRules.zoneForMaxHeartRate(it, maxHr) }
    val pct = hr?.let { HealthRules.heartRatePercent(it, maxHr) } ?: 0
    // Gauge spans ~40%→100% of max HR, so resting sits near the start (low, lower-left) like the native.
    val frac = ((pct - 40f) / 60f).coerceIn(0f, 1f)

    val subline = when {
        zone != null -> zoneName(zone)
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
                HeartGauge(frac = frac, track = track, fill = Teal, modifier = Modifier.size(74.dp))
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
 * Native-style HR gauge: a ~320° arc with a small gap at the bottom, a lavender [track], a [fill]
 * (teal) up to [frac] of the way round, and a white dot (teal core) marking the value.
 */
@Composable
private fun HeartGauge(frac: Float, track: Color, fill: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 6.dp.toPx()
        val r = (min(size.width, size.height) - stroke) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val topLeft = Offset(cx - r, cy - r)
        val arcSize = Size(r * 2f, r * 2f)
        val start = 110f
        val total = 320f
        val f = frac.coerceIn(0f, 1f)

        // Segmented (dashed, rounded) lavender track.
        drawArc(
            track.copy(alpha = 0.7f), start, total, false, topLeft, arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 3.5f, stroke * 2f), 0f)),
        )
        // Smooth continuous teal fill up to the current value.
        drawArc(fill, start, total * f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))

        // White dot marking the value (teal core).
        val ang = (start + total * f) * (PI / 180.0)
        val dot = Offset(cx + r * cos(ang).toFloat(), cy + r * sin(ang).toFloat())
        drawCircle(Color.White, stroke * 0.82f, dot)
        drawCircle(fill, stroke * 0.42f, dot)

        // Cyan outline heart in the center.
        val hs = r * 0.5f
        val heart = Path().apply {
            moveTo(cx, cy - hs * 0.25f)
            cubicTo(cx - hs * 0.5f, cy - hs * 0.95f, cx - hs * 1.3f, cy - hs * 0.1f, cx, cy + hs * 0.7f)
            cubicTo(cx + hs * 1.3f, cy - hs * 0.1f, cx + hs * 0.5f, cy - hs * 0.95f, cx, cy - hs * 0.25f)
        }
        drawPath(heart, fill, style = Stroke(width = stroke * 0.62f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
