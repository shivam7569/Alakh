package com.andy.alakh.presentation.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.shared.data.ExerciseListItem

private val Accent = Color(0xFF34C796)

/**
 * The exercise catalog: a search bar, then exercises grouped into muscle-group sections.
 * Typing filters to a flat result list. Tapping a row hands the item back via [onSelect]
 * (browse -> detail, or workout -> add to session).
 */
@Composable
fun ExercisesScreen(onSelect: ((ExerciseListItem) -> Unit)? = null) {
    val viewModel: ExercisesViewModel = viewModel()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item(key = "search", contentType = "search") {
                SearchBar(query = query, onQueryChange = viewModel::setQuery)
            }
            items(
                items = entries,
                key = { entry ->
                    when (entry) {
                        is CatalogEntry.Header -> "h:${entry.title}"
                        is CatalogEntry.Item -> entry.exercise.id
                    }
                },
                contentType = { it is CatalogEntry.Header },
            ) { entry ->
                when (entry) {
                    is CatalogEntry.Header -> SectionHeader(entry.title)
                    is CatalogEntry.Item -> ExerciseRow(entry.exercise) { onSelect?.invoke(entry.exercise) }
                }
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        if (query.isBlank()) "Loading catalog…" else "No matches for \"$query\"",
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x1FFFFFFF))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔍", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search exercises", color = Color(0xFF9AA3A0), fontSize = 15.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 12.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleSmall,
        color = Accent,
    )
}

@Composable
private fun ExerciseRow(item: ExerciseListItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            item.primaryMuscles.joinToString(", ") { it.displayName },
            style = MaterialTheme.typography.bodySmall,
            color = Accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
