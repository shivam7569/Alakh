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

/**
 * Catalog browsing. By default it's a short list of tappable muscle-group headers (collapsed) — tap
 * one to expand its exercises (single-open accordion), so you're never scrolling all 800+ at once.
 * The search icon expands an in-app field that filters to flat results by name/category/muscle.
 */
@Composable
fun ExercisesScreen(onSelect: ((ExerciseListItem) -> Unit)? = null) {
    val viewModel: ExercisesViewModel = viewModel()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var searching by remember { mutableStateOf(false) }
    var expandedGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searching) { if (searching) runCatching { focusRequester.requestFocus() } }

    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item(key = "search", contentType = "search") {
                if (searching) {
                    SearchField(
                        query = query,
                        onQueryChange = viewModel::setQuery,
                        focusRequester = focusRequester,
                        onClose = { searching = false; viewModel.setQuery("") },
                    )
                } else {
                    SearchIcon(onClick = { searching = true })
                }
            }

            if (query.isNotBlank()) {
                items(results, key = { it.id }, contentType = { "item" }) { item ->
                    ExerciseRow(item) { onSelect?.invoke(item) }
                }
                if (results.isEmpty()) {
                    item { EmptyNote("No matches for \"$query\"") }
                }
            } else {
                sections.forEach { section ->
                    item(key = "h:${section.group.name}", contentType = "header") {
                        SectionHeader(
                            group = section.group,
                            count = section.items.size,
                            expanded = expandedGroup == section.group,
                            onClick = {
                                expandedGroup = if (expandedGroup == section.group) null else section.group
                            },
                        )
                    }
                    if (expandedGroup == section.group) {
                        items(section.items, key = { it.id }, contentType = { "item" }) { item ->
                            ExerciseRow(item) { onSelect?.invoke(item) }
                        }
                    }
                }
                if (sections.isEmpty()) {
                    item { EmptyNote("Loading catalog…") }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(group: MuscleGroup, count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.displayName.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = AlakhAccent,
            modifier = Modifier.weight(1f),
        )
        Text("$count", color = Color(0xFF9AA3A0), fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(if (expanded) "▾" else "▸", color = AlakhAccent, fontSize = 13.sp)
    }
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
            color = AlakhAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchIcon(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x1FFFFFFF))
                .clickable { onClick() }
                .padding(10.dp),
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
                if (query.isEmpty()) {
                    Text("Search", color = Color(0xFF9AA3A0), fontSize = 14.sp)
                }
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

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF9AA3A0),
    )
}
