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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.theme.AlakhAccent
import com.andy.alakh.shared.data.ExerciseListItem
import com.andy.alakh.shared.model.MuscleGroup

/** Generous top/bottom padding so the round screen never clips the first/last row. */
internal val CatalogPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 30.dp, bottom = 48.dp)

/**
 * Top level of the catalog: pick a muscle group (its own short page), or search across everything.
 * Selecting a group opens [onOpenGroup]; a search result opens [onSelectExercise].
 */
@Composable
fun ExercisesScreen(
    onOpenGroup: (MuscleGroup) -> Unit,
    onSelectExercise: (ExerciseListItem) -> Unit,
) {
    val viewModel: ExercisesViewModel = viewModel()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = CatalogPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "search", contentType = "search") {
                ExpandingSearch(query = query, onQueryChange = viewModel::setQuery)
            }
            if (query.isNotBlank()) {
                items(results, key = { it.id }, contentType = { "item" }) { item ->
                    ExerciseRow(item) { onSelectExercise(item) }
                }
                if (results.isEmpty()) item { EmptyNote("No matches for \"$query\"") }
            } else {
                item(key = "title", contentType = "title") {
                    Text(
                        "MUSCLE GROUPS",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9AA3A0),
                    )
                }
                items(sections, key = { it.group.name }, contentType = { "group" }) { section ->
                    GroupCard(section.group, section.items.size) { onOpenGroup(section.group) }
                }
                if (sections.isEmpty()) item { EmptyNote("Loading catalog…") }
            }
        }
    }
}

@Composable
private fun GroupCard(group: MuscleGroup, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            group.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = AlakhAccent,
            modifier = Modifier.weight(1f),
        )
        Text("$count", color = Color(0xFF9AA3A0), fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Text("›", color = AlakhAccent, fontSize = 18.sp)
    }
}

// --- shared catalog UI (used by ExercisesScreen and MuscleExercisesScreen) ---

@Composable
fun ExerciseRow(item: ExerciseListItem, onClick: () -> Unit) {
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
            color = AlakhAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EmptyNote(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF9AA3A0),
    )
}

@Composable
fun ExpandingSearch(query: String, onQueryChange: (String) -> Unit) {
    var searching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searching) { if (searching) runCatching { focusRequester.requestFocus() } }

    if (searching) {
        SearchField(query, onQueryChange, focusRequester) { searching = false; onQueryChange("") }
    } else {
        SearchIcon { searching = true }
    }
}

@Composable
private fun SearchIcon(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier.clip(CircleShape).background(Color(0x1FFFFFFF)).clickable { onClick() }.padding(10.dp),
            contentAlignment = Alignment.Center,
        ) { Text("🔍", fontSize = 16.sp) }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onClose: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x26FFFFFF))
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔍", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search", color = Color(0xFF9AA3A0), fontSize = 14.sp)
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(AlakhAccent),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            Box(
                modifier = Modifier.clip(CircleShape).clickable { onClose() }.padding(5.dp),
                contentAlignment = Alignment.Center,
            ) { Text("✕", fontSize = 12.sp, color = Color.White) }
        }
    }
}
