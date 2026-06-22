package com.andy.alakh.presentation.workout

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.shared.model.SetType

/**
 * Logs one set. Weight is adjustable with the rotating crown (and +/- buttons); reps, RPE and
 * set type via taps. Everything pre-fills from the previous set ("accept or nudge"), which is
 * the friction-free model that makes on-watch logging viable.
 */
@Composable
fun SetEntryScreen(onLogged: () -> Unit) {
    val index = ActiveWorkout.editingIndex
    val draftExercise = ActiveWorkout.exerciseAt(index)
    val last = ActiveWorkout.lastSet(index)

    var weight by remember { mutableStateOf(last?.weightKg ?: 20.0) }
    var reps by remember { mutableStateOf(last?.reps ?: 8) }
    var rpe by remember { mutableStateOf(last?.rpe ?: 8.0) }
    var setType by remember { mutableStateOf(last?.setType ?: SetType.NORMAL) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = draftExercise?.exercise?.name ?: "Set",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            StepperRow(
                label = "Weight (kg) · crown",
                value = fmt(weight),
                onMinus = { weight = (weight - 2.5).coerceAtLeast(0.0) },
                onPlus = { weight += 2.5 },
                modifier = Modifier
                    .onRotaryScrollEvent { event ->
                        val step = if (event.verticalScrollPixels > 0f) 2.5 else -2.5
                        weight = (weight + step).coerceAtLeast(0.0)
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
            )
            StepperRow(
                label = "Reps",
                value = reps.toString(),
                onMinus = { reps = (reps - 1).coerceAtLeast(0) },
                onPlus = { reps += 1 },
            )
            StepperRow(
                label = "RPE",
                value = fmt(rpe),
                onMinus = { rpe = (rpe - 0.5).coerceAtLeast(1.0) },
                onPlus = { rpe = (rpe + 0.5).coerceAtMost(10.0) },
            )

            Button(onClick = { setType = nextType(setType) }, modifier = Modifier.fillMaxWidth()) {
                Text("Type: ${labelOf(setType)}")
            }
            Button(
                onClick = {
                    if (index >= 0) ActiveWorkout.logSet(index, DraftSet(setType, weight, reps, rpe))
                    onLogged()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Log set") }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onMinus) { Text("−") }
            Text(value, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onPlus) { Text("+") }
        }
    }
}

private fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

private fun nextType(t: SetType): SetType {
    val all = SetType.values()
    return all[(t.ordinal + 1) % all.size]
}

private fun labelOf(t: SetType): String = t.name.lowercase().replaceFirstChar { it.uppercase() }
