package com.andy.alakh.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices

@Composable
fun HomeScreen(
    onWorkout: () -> Unit,
    onBreathing: () -> Unit,
    onExercises: () -> Unit,
) {
    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "ALAKH",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF34C796),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(2.dp))
            Button(onClick = onWorkout, modifier = Modifier.fillMaxWidth()) {
                Text("Workout", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            Button(onClick = onBreathing, modifier = Modifier.fillMaxWidth()) {
                Text("Breathing", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            Button(onClick = onExercises, modifier = Modifier.fillMaxWidth()) {
                Text("Exercises", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(onWorkout = {}, onBreathing = {}, onExercises = {})
}
