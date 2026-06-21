package com.andy.alakh.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.shared.model.ExerciseStatus
import com.andy.alakh.shared.model.WorkoutType

@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel = viewModel()) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("❤️ ${metrics.heartRateBpm?.toString() ?: "--"} bpm")
            Text("⏱️ ${metrics.elapsedMs / 1000}s")
            Text(status.name)

            val active = status == ExerciseStatus.ACTIVE || status == ExerciseStatus.PAUSED
            if (active) {
                Button(onClick = { viewModel.stop() }) { Text("Stop") }
            } else {
                Button(onClick = { viewModel.start(WorkoutType.WALKING) }) { Text("Start walk") }
            }
        }
    }
}
