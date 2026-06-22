package com.andy.alakh.presentation.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.shared.data.ExerciseListItem

/**
 * The exercise catalog as a scrollable list. One screen, two uses:
 *  - browse (onSelect == null): non-tappable rows.
 *  - picker (onSelect != null): tappable rows that hand the chosen item back.
 *
 * Rows are intentionally lightweight (a clickable surface + two clipped text lines) rather than
 * full Material Buttons, so scrolling 800+ items stays smooth.
 */
@Composable
fun ExercisesScreen(onSelect: ((ExerciseListItem) -> Unit)? = null) {
    val viewModel: ExercisesViewModel = viewModel()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    ScreenScaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 28.dp),
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
            items(items = exercises, key = { it.id }) { item ->
                ExerciseRow(item = item, onClick = onSelect?.let { sel -> { sel(item) } })
            }
        }
    }
}

@Composable
private fun ExerciseRow(item: ExerciseListItem, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(18.dp)
    val base = Modifier.fillMaxWidth().clip(shape)
    val rowModifier = (if (onClick != null) base.clickable { onClick() } else base)
        .background(Color(0x14FFFFFF))
        .padding(horizontal = 12.dp, vertical = 8.dp)

    Column(modifier = rowModifier) {
        Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            item.primaryMuscles.joinToString(", ") { it.displayName },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
