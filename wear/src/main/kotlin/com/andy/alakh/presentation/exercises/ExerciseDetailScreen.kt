package com.andy.alakh.presentation.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.components.BodyHeatmap
import com.andy.alakh.shared.data.ExerciseListItem

/** Carries the tapped catalog item to the detail screen without typed navigation arguments. */
object ExerciseDetailHolder {
    var item: ExerciseListItem? = null
}

private val Accent = Color(0xFF22B8CF)

/** Read-only exercise view: name, the muscle heatmap, and the muscle breakdown. */
@Composable
fun ExerciseDetailScreen() {
    val item = ExerciseDetailHolder.item

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item?.name ?: "Exercise",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            if (item != null) {
                BodyHeatmap(
                    primary = item.primaryMuscles.toSet(),
                    secondary = item.secondaryMuscles.toSet(),
                    modifier = Modifier.fillMaxWidth().height(156.dp),
                )

                if (item.primaryMuscles.isNotEmpty()) {
                    Text("PRIMARY", style = MaterialTheme.typography.labelSmall, color = Accent)
                    Text(
                        item.primaryMuscles.joinToString(", ") { it.displayName },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
                if (item.secondaryMuscles.isNotEmpty()) {
                    Text("SECONDARY", style = MaterialTheme.typography.labelSmall)
                    Text(
                        item.secondaryMuscles.joinToString(", ") { it.displayName },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    item.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
