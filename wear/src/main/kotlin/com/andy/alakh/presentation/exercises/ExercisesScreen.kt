package com.andy.alakh.presentation.exercises

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.input.RemoteInputIntentHelper
import com.andy.alakh.presentation.theme.colorForGroup
import com.andy.alakh.shared.data.ExerciseListItem
import com.andy.alakh.shared.model.MuscleGroup

private const val QUERY_KEY = "alakh_search_query"

/**
 * The exercise catalog: a compact search icon (taps open the watch's full-screen voice/keyboard
 * input — no clipping at the round edge), then exercises grouped into muscle-group sections, each
 * painted with its group color. Typing filters by name, category, OR muscle group.
 */
@Composable
fun ExercisesScreen(onSelect: ((ExerciseListItem) -> Unit)? = null) {
    val viewModel: ExercisesViewModel = viewModel()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = RemoteInput.getResultsFromIntent(result.data)?.getCharSequence(QUERY_KEY)?.toString()
        if (text != null) viewModel.setQuery(text)
    }
    val openSearch: () -> Unit = {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        val inputs = listOf(RemoteInput.Builder(QUERY_KEY).setLabel("Search exercises").build())
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, inputs)
        launcher.launch(intent)
    }

    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item(key = "search", contentType = "search") {
                SearchControl(query = query, onOpen = openSearch, onClear = { viewModel.setQuery("") })
            }
            items(
                items = entries,
                key = { entry ->
                    when (entry) {
                        is CatalogEntry.Header -> "h:${entry.group.name}"
                        is CatalogEntry.Item -> entry.exercise.id
                    }
                },
                contentType = { it is CatalogEntry.Header },
            ) { entry ->
                when (entry) {
                    is CatalogEntry.Header -> SectionHeader(entry.group)
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
private fun SearchControl(query: String, onOpen: () -> Unit, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1FFFFFFF))
                .clickable { onOpen() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔍", fontSize = 15.sp)
            if (query.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    query,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp),
                )
            }
        }
        if (query.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x26FFFFFF))
                    .clickable { onClear() }
                    .padding(7.dp),
                contentAlignment = Alignment.Center,
            ) { Text("✕", fontSize = 12.sp, color = Color.White) }
        }
    }
}

@Composable
private fun SectionHeader(group: MuscleGroup) {
    Text(
        text = group.displayName.uppercase(),
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 12.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleSmall,
        color = colorForGroup(group),
    )
}

@Composable
private fun ExerciseRow(item: ExerciseListItem, onClick: () -> Unit) {
    val color = colorForGroup(item.primaryMuscles.firstOrNull())
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
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
