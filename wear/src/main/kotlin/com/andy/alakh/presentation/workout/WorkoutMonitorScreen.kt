package com.andy.alakh.presentation.workout

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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

private val Muted = Color(0xFF9AA3A0)

private fun zoneColor(zone: HeartRateZone): Color = when (zone) {
    HeartRateZone.REST -> Color(0xFF9AA3A0)
    HeartRateZone.ZONE_1 -> Color(0xFF4F9DE0)
    HeartRateZone.ZONE_2 -> Color(0xFF34C796)
    HeartRateZone.ZONE_3 -> Color(0xFFE0C030)
    HeartRateZone.ZONE_4 -> Color(0xFFE0A030)
    HeartRateZone.ZONE_5 -> Color(0xFFE0573E)
}

private fun zoneLabel(zone: HeartRateZone): String = when (zone) {
    HeartRateZone.REST -> "REST"
    HeartRateZone.ZONE_1 -> "ZONE 1 · warm up"
    HeartRateZone.ZONE_2 -> "ZONE 2 · fat burn"
    HeartRateZone.ZONE_3 -> "ZONE 3 · cardio"
    HeartRateZone.ZONE_4 -> "ZONE 4 · hard"
    HeartRateZone.ZONE_5 -> "ZONE 5 · peak"
}

private fun fmtTime(elapsedMs: Long): String {
    val total = (elapsedMs / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

/** Full-screen live workout monitor: HR + zone ring, calories, and elapsed time. */
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
        WorkoutSensors.start(context) // (re)start tracking now that the sensor permission may be granted
    }

    val hr = metrics.heartRateBpm
    val zone = hr?.let { HealthRules.zoneForMaxHeartRate(it, maxHr) }
    val pct = hr?.let { HealthRules.heartRatePercent(it, maxHr) } ?: 0
    val accent = zone?.let { zoneColor(it) } ?: Muted

    val statusText = when {
        zone != null -> zoneLabel(zone)
        bodySensorsMissing -> "Body Sensors permission needed"
        else -> diagnostic ?: "Waiting for heart rate…"
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Text(fmtTime(metrics.elapsedMs), style = MaterialTheme.typography.bodySmall, color = Muted)

            Box(contentAlignment = Alignment.Center) {
                ZoneRing(percent = pct, color = accent, modifier = Modifier.size(116.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        hr?.toString() ?: "—",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text("BPM", fontSize = 11.sp, color = Muted, letterSpacing = 2.sp)
                }
            }

            Text(
                statusText,
                fontSize = 12.sp,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat("${metrics.calories?.toInt() ?: 0}", "kcal")
                Stat(if (hr != null) "$pct%" else "—", "of max")
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
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(label, fontSize = 10.sp, color = Muted)
    }
}

/** A circular gauge filled to [percent] of max HR, colored by the current zone. */
@Composable
private fun ZoneRing(percent: Int, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 7.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = color.copy(alpha = 0.18f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
