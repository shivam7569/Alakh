package com.andy.alakh.presentation.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.components.BodyHeatmap
import com.andy.alakh.shared.data.ExerciseListItem
import com.andy.alakh.shared.data.WorkoutRepository
import com.andy.alakh.shared.model.ExerciseStats
import kotlin.math.roundToInt

/** Carries the tapped catalog item to the detail screen without typed navigation arguments. */
object ExerciseDetailHolder {
    var item: ExerciseListItem? = null
}

private val Accent = Color(0xFF34C796)
private val Muted = Color(0xFF9AA3A0)

/** Read-only exercise view: name, the muscle heatmap, the muscle breakdown, and personal records. */
@Composable
fun ExerciseDetailScreen() {
    val item = ExerciseDetailHolder.item
    val context = LocalContext.current
    val stats by produceState<ExerciseStats?>(initialValue = null, item?.id) {
        val id = item?.id
        value = if (id != null) runCatching { WorkoutRepository.get(context).exerciseStats(id) }.getOrNull() else null
    }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 30.dp, bottom = 46.dp),
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
                    Label("PRIMARY")
                    MuscleText(item.primaryMuscles.joinToString(", ") { it.displayName }, Color.White)
                }
                if (item.secondaryMuscles.isNotEmpty()) {
                    Label("SECONDARY")
                    MuscleText(item.secondaryMuscles.joinToString(", ") { it.displayName }, Muted)
                }

                StatsSection(stats)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Accent,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun MuscleText(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, textAlign = TextAlign.Center)
}

@Composable
private fun StatsSection(stats: ExerciseStats?) {
    Label("RECORDS")
    if (stats == null || stats.totalSets == 0) {
        Text(
            "Log a few sets to build your records.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatCard("Est. 1RM", kg(stats.estimatedOneRmKg), Modifier.weight(1f))
        StatCard("Heaviest", kg(stats.heaviestWeightKg), Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatCard("Best set", kg(stats.bestSetVolumeKg), Modifier.weight(1f))
        StatCard("Best session", kg(stats.bestSessionVolumeKg), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Accent, textAlign = TextAlign.Center)
    }
}

private fun kg(value: Double?): String = value?.let { "${it.roundToInt()} kg" } ?: "—"
