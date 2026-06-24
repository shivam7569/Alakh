package com.andy.alakh.presentation.workout

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andy.alakh.health.UserProfile
import com.andy.alakh.health.WorkoutSensors
import com.andy.alakh.shared.data.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

private val Accent = Color(0xFF34C796)
private val Muted = Color(0xFF9AA3A0)

/**
 * The workout. Empty → start from a routine or build one ad-hoc. Once started, it's a two-page
 * session you swipe between: the exercise list (add / remove / replace, log sets) and the live
 * heart-rate monitor. Starting/finishing the sensor service is wired in AlakhApp / here.
 */
@Composable
fun WorkoutScreen(
    onStartAdHoc: () -> Unit,
    onFromRoutine: () -> Unit,
    onAddExercise: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onReplaceExercise: (Int) -> Unit,
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
    LaunchedEffect(Unit) { RestTimer.setDefaultDuration(UserProfile.restDurationSec(context)) }

    val current = session
    if (current == null) {
        ScreenScaffold {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Text("Workout", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onFromRoutine, modifier = Modifier.fillMaxWidth()) {
                    Text("From routine", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1)
                }
                Button(onClick = onStartAdHoc, modifier = Modifier.fillMaxWidth()) {
                    Text("Add exercises", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        return
    }

    PagerWithMonitor {
        ExercisesPage(
                session = current,
                restRemaining = restRemaining,
                onRestAdjust = { delta ->
                    RestTimer.addTime(delta)
                    UserProfile.setRestDurationSec(context, RestTimer.durationSec)
                },
                onRestSkip = { RestTimer.skip() },
                onAddExercise = onAddExercise,
                onOpenExercise = onOpenExercise,
                onReplaceExercise = onReplaceExercise,
                onRemoveExercise = { ActiveWorkout.removeExercise(it) },
                onFinish = {
                    val draft = ActiveWorkout.draft.value
                    if (draft != null && draft.exercises.any { it.sets.isNotEmpty() }) {
                        val log = draft.toSessionLog()
                        scope.launch(Dispatchers.IO) {
                            runCatching { WorkoutRepository.get(context).saveSession(log) }
                        }
                    }
                    RestTimer.skip()
                    WorkoutSensors.stop(context)
                    ActiveWorkout.finish()
                    onFinished()
                },
        )
    }
}

@Composable
private fun ExercisesPage(
    session: DraftSession,
    restRemaining: Int,
    onRestAdjust: (Int) -> Unit,
    onRestSkip: () -> Unit,
    onAddExercise: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onReplaceExercise: (Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 46.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (restRemaining > 0) {
                item(key = "rest") { RestBanner(restRemaining, onRestAdjust, onRestSkip) }
            }
            item(key = "title") {
                Text(
                    "Workout · ${session.exercises.size}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            itemsIndexed(session.exercises) { index, draftExercise ->
                ExerciseRow(
                    exercise = draftExercise,
                    onOpen = { onOpenExercise(index) },
                    onReplace = { onReplaceExercise(index) },
                    onRemove = { onRemoveExercise(index) },
                )
            }
            item(key = "add") {
                Button(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) { Text("+ Add exercise") }
            }
            item(key = "finish") {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish") }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: DraftExercise,
    onOpen: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x14FFFFFF)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable { onOpen() }.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
            Text(
                if (exercise.sets.isEmpty()) "Tap to log a set" else "${exercise.sets.size} sets · ${summary(exercise.sets.last())}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        IconButton("↔", onReplace)
        IconButton("✕", onRemove)
    }
}

@Composable
private fun IconButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0x1FFFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, color = Color.White, fontSize = 13.sp) }
}

@Composable
private fun RestBanner(remaining: Int, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x2234C796))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Rest ${fmtClock(remaining)}", color = Accent, modifier = Modifier.weight(1f))
        Chip("−15") { onAdjust(-15) }
        Chip("+15") { onAdjust(15) }
        Chip("Skip") { onSkip() }
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
