package com.andy.alakh.presentation.workout

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andy.alakh.health.WorkoutSensors
import com.andy.alakh.shared.data.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

private val Accent = Color(0xFF34C796)

/**
 * The active workout. Empty → start from a routine or build one ad-hoc. In progress → a live-monitor
 * shortcut, the rest banner, and the list of exercises + sets, plus Add exercise and Finish. Starting
 * is centralized in [com.andy.alakh.presentation.AlakhApp] (permissions + sensor service); Finish
 * stops the sensor service here.
 */
@Composable
fun WorkoutScreen(
    onStartAdHoc: () -> Unit,
    onFromRoutine: () -> Unit,
    onAddExercise: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onOpenMonitor: () -> Unit,
    onFinished: () -> Unit,
) {
    val session by ActiveWorkout.draft.collectAsStateWithLifecycle()
    val restRemaining by RestTimer.remaining.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Buzz once when a rest countdown reaches zero.
    var wasResting by remember { mutableStateOf(false) }
    LaunchedEffect(restRemaining) {
        if (restRemaining == 0 && wasResting) vibrate(context)
        wasResting = restRemaining > 0
    }

    ScreenScaffold {
        val current = session
        if (current == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Text("Workout", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Start from a routine or build one on your wrist",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onFromRoutine, modifier = Modifier.fillMaxWidth()) { Text("From routine") }
                Button(onClick = onStartAdHoc, modifier = Modifier.fillMaxWidth()) { Text("Add exercises") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 46.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (restRemaining > 0) {
                    item(key = "rest") { RestBanner(restRemaining) }
                }
                item {
                    Text(
                        "Workout · ${current.exercises.size}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item {
                    Button(onClick = onOpenMonitor, modifier = Modifier.fillMaxWidth()) { Text("♥  Monitor") }
                }
                itemsIndexed(current.exercises) { index, draftExercise ->
                    Button(onClick = { onOpenExercise(index) }, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(draftExercise.name)
                            Text(
                                if (draftExercise.sets.isEmpty()) {
                                    "Tap to log a set"
                                } else {
                                    "${draftExercise.sets.size} sets · ${summary(draftExercise.sets.last())}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                item {
                    Button(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                        Text("+ Add exercise")
                    }
                }
                item {
                    Button(
                        onClick = {
                            val draft = ActiveWorkout.draft.value
                            if (draft != null && draft.exercises.any { it.sets.isNotEmpty() }) {
                                val log = draft.toSessionLog()
                                scope.launch(Dispatchers.IO) {
                                    runCatching { WorkoutRepository.get(context).saveSession(log) }
                                }
                            }
                            RestTimer.skip()
                            WorkoutSensors.stop(context) // end the Health Services exercise + foreground service
                            ActiveWorkout.finish()
                            onFinished()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Finish") }
                }
            }
        }
    }
}

@Composable
private fun RestBanner(remaining: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x2234C796))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Rest ${fmtClock(remaining)}", color = Accent, modifier = Modifier.weight(1f))
        Chip("+15") { RestTimer.addTime(15) }
        Chip("Skip") { RestTimer.skip() }
    }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1FFFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun fmtClock(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)

private fun summary(set: DraftSet): String {
    val weight = set.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "–"
    val reps = set.reps?.toString() ?: "–"
    return "${weight}kg × $reps"
}

private fun vibrate(context: Context) {
    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    val vibrator = manager?.defaultVibrator ?: return
    if (!vibrator.hasVibrator()) return
    runCatching {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), intArrayOf(0, 200, 0, 220), -1))
    }
}
