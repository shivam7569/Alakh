package com.andy.alakh.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/**
 * The active workout. Empty → a single "Start workout" call to action. In progress → the list
 * of exercises with a one-line summary of their sets, plus "Add exercise" and "Finish".
 */
@Composable
fun WorkoutScreen(
    onAddExercise: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onFinished: () -> Unit,
) {
    val session by ActiveWorkout.draft.collectAsStateWithLifecycle()

    ScreenScaffold {
        val current = session
        if (current == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                Text("Workout", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Build a session on your wrist",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = { ActiveWorkout.startIfNeeded(); onAddExercise() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start workout") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 46.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Text(
                        "Workout · ${current.exercises.size}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                    )
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
                        onClick = { ActiveWorkout.finish(); onFinished() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Finish") }
                }
            }
        }
    }
}

private fun summary(set: DraftSet): String {
    val weight = set.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "–"
    val reps = set.reps?.toString() ?: "–"
    return "${weight}kg × $reps"
}
