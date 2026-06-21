package com.andy.alakh.presentation.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/** First read-from-the-database screen: a scrollable list of the whole exercise catalog. */
@Composable
fun ExercisesScreen(viewModel: ExercisesViewModel = viewModel()) {
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
                )
            }
            items(items = exercises, key = { it.id }) { ex ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(ex.name)
                    Text(ex.primaryMuscles.joinToString(", ") { it.displayName })
                }
            }
        }
    }
}
