package com.andy.alakh.presentation.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.shared.model.Exercise

/**
 * The exercise catalog as a scrollable list. Two uses, one screen:
 *  - browse (onSelect == null): plain, non-tappable rows.
 *  - picker (onSelect != null): tappable rows that hand the chosen exercise back to the caller.
 */
@Composable
fun ExercisesScreen(onSelect: ((Exercise) -> Unit)? = null) {
    val viewModel: ExercisesViewModel = viewModel()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    text = if (exercises.isEmpty()) "Loading catalog…" else "Exercises · ${exercises.size}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(items = exercises, key = { it.id }) { exercise ->
                if (onSelect != null) {
                    Button(onClick = { onSelect(exercise) }, modifier = Modifier.fillMaxWidth()) {
                        ExerciseRow(exercise)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
                        ExerciseRow(exercise)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise) {
    Column {
        Text(exercise.name)
        Text(
            exercise.primaryMuscles.joinToString(", ") { it.displayName },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
