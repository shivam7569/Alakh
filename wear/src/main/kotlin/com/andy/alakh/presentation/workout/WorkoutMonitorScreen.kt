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
import androidx.compose.ui.graphics.StrokeCap
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

private fun zoneColor(zone: HeartRateZone): Color = when (zone) {
    HeartRateZone.REST -> Color(0xFF9AA3A0)
    HeartRateZone.ZONE_1 -> Color(0xFF4F9DE0)
    HeartRateZone.ZONE_2 -> Color(0xFF34C796)
    HeartRateZone.ZONE_3 -> Color(0xFFE0C030)
    HeartRateZone.ZONE_4 -> Color(0xFFE0A030)
    HeartRateZone.ZONE_5 -> Color(0xFFE0573E)
}

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
 * Live workout monitor, styled after the watch's native heart-rate screen: a small zone ring with a
 * heart glyph, the "Heart rate" label, the big BPM value, the zone name, and a calories + time footer.
 */
@Composable
fun WorkoutMonitorScreen() {
    val vm: WorkoutViewModel = viewModel()
    val metrics by vm.metrics.collectAsStateWithLifecycle()
    val diagnostic by vm.diagnostic.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val maxHr = remember { UserProfile.maxHeartRate(context) }

    var bodySensorsMissing by remember { mutableStateOf(PermissionHelper.missingBodySensors(context)) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        bodySensorsMissing = PermissionHelper.missingBodySensors(context)
        WorkoutSensors.start(context)
    }

    val hr = metrics.heartRateBpm
    val zone = hr?.let { HealthRules.zoneForMaxHeartRate(it, maxHr) }
    val pct = hr?.let { HealthRules.heartRatePercent(it, maxHr) } ?: 0
    val accent = zone?.let { zoneColor(it) } ?: Muted

    // Below the BPM: the zone name when we have HR, otherwise a short status (no raw error text).
    val subline = when {
        zone != null -> zoneName(zone)
        bodySensorsMissing -> "Body Sensors needed"
        else -> diagnostic ?: "Waiting for heart rate…"
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Box(contentAlignment = Alignment.Center) {
                ZoneGauge(percent = pct, color = accent, modifier = Modifier.size(72.dp))
                Text("♥", color = accent, fontSize = 17.sp)
            }
            Text("Heart rate", fontSize = 13.sp, color = Color(0xFFB9A7E8))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(hr?.toString() ?: "—", fontSize = 46.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(" bpm", fontSize = 16.sp, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(
                subline,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.sp,
                color = accent,
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
 * The native-style HR gauge: a 270° arc with a gap at the bottom, a dim full-range track, a colored
 * fill up to the current % of max HR, and a dot marking the value — matching the watch's HR screen.
 */
@Composable
private fun ZoneGauge(percent: Int, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 6.dp.toPx()
        val r = (min(size.width, size.height) - stroke) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val topLeft = Offset(cx - r, cy - r)
        val arcSize = Size(r * 2f, r * 2f)
        val start = 135f
        val total = 270f
        val frac = percent.coerceIn(0, 100) / 100f

        drawArc(color.copy(alpha = 0.18f), start, total, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(color, start, total * frac, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))

        val ang = (start + total * frac) * (PI / 180.0)
        val dot = Offset(cx + r * cos(ang).toFloat(), cy + r * sin(ang).toFloat())
        drawCircle(Color.White, stroke * 0.72f, dot)
        drawCircle(color, stroke * 0.40f, dot)
    }
}
