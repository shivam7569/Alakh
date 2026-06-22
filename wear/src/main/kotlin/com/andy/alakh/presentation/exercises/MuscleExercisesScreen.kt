package com.andy.alakh.presentation.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.theme.AlakhAccent
import com.andy.alakh.shared.data.ExerciseListItem
import com.andy.alakh.shared.model.MuscleGroup

/** Carries the chosen group (and browse-vs-pick mode) to the per-group page without nav args. */
object CatalogNav {
    var group: MuscleGroup? = null
    var picking: Boolean = false
}

/** A single muscle group's exercises, with an in-group search. Swipe right to go back to the groups. */
@Composable
fun MuscleExercisesScreen(group: MuscleGroup, onSelectExercise: (ExerciseListItem) -> Unit) {
    val viewModel: ExercisesViewModel = viewModel()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val all = sections.firstOrNull { it.group == group }?.items ?: emptyList()
    val items = if (query.isBlank()) all
    else all.filter { it.name.contains(query.trim(), ignoreCase = true) }

    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = CatalogPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "title", contentType = "title") {
                Text(
                    group.displayName,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = AlakhAccent,
                )
            }
            item(key = "search", contentType = "search") {
                ExpandingSearch(query = query, onQueryChange = { query = it })
            }
            items(items, key = { it.id }, contentType = { "item" }) { item ->
                ExerciseRow(item) { onSelectExercise(item) }
            }
            if (items.isEmpty()) {
                item { EmptyNote(if (all.isEmpty()) "Loading…" else "No matches") }
            }
        }
    }
}
