package com.andy.alakh.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/**
 * Routine picker. Routines are planned on the phone app and synced to the watch (feature #4), so for
 * now this is an empty state that points the user there, with a shortcut to build a session ad-hoc.
 */
@Composable
fun RoutinesScreen(onAddExercises: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            Text("Routines", style = MaterialTheme.typography.titleMedium)
            Text(
                "No routines yet. Create them in the Alakh phone app and they'll sync here.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAddExercises, modifier = Modifier.fillMaxWidth()) { Text("Add exercises instead") }
        }
    }
}
