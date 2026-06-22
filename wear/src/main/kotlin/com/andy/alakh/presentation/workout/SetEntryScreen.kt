package com.andy.alakh.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.andy.alakh.presentation.components.BodyHeatmap
import com.andy.alakh.presentation.components.TickButton
import com.andy.alakh.shared.model.SetType

private val Accent = Color(0xFF34C796)
private val Muted = Color(0xFF9AA3A0)

/**
 * Logs one set. Weight is adjustable with the crown (and the ± buttons); reps, RPE and set type by
 * tap. Pre-fills from the previous set. The body heatmap up top shows what's being worked.
 */
@Composable
fun SetEntryScreen(onLogged: () -> Unit) {
    val index = ActiveWorkout.editingIndex
    val draftExercise = ActiveWorkout.exerciseAt(index)
    val last = ActiveWorkout.lastSet(index)

    var weight by remember { mutableStateOf(last?.weightKg ?: 20.0) }
    var reps by remember { mutableStateOf(last?.reps ?: 8) }
    var rpe by remember { mutableStateOf(last?.rpe ?: 8.0) }
    var setType by remember { mutableStateOf(last?.setType ?: SetType.NORMAL) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = draftExercise?.name ?: "Set",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )

            if (draftExercise != null) {
                BodyHeatmap(
                    primary = draftExercise.primaryMuscles.toSet(),
                    secondary = draftExercise.secondaryMuscles.toSet(),
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                )
            }

            StepperCard(
                label = "WEIGHT",
                value = fmt(weight),
                unit = "kg",
                onMinus = { weight = (weight - 2.5).coerceAtLeast(0.0) },
                onPlus = { weight += 2.5 },
                modifier = Modifier
                    .onRotaryScrollEvent { event ->
                        val step = if (event.verticalScrollPixels > 0f) 2.5 else -2.5
                        weight = (weight + step).coerceAtLeast(0.0)
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
            )
            StepperCard(
                label = "REPS",
                value = reps.toString(),
                unit = null,
                onMinus = { reps = (reps - 1).coerceAtLeast(0) },
                onPlus = { reps += 1 },
            )
            StepperCard(
                label = "RPE",
                value = fmt(rpe),
                unit = null,
                onMinus = { rpe = (rpe - 0.5).coerceAtLeast(1.0) },
                onPlus = { rpe = (rpe + 0.5).coerceAtMost(10.0) },
            )

            TypeChip(setType) { setType = nextType(setType) }

            Spacer(Modifier.height(2.dp))
            TickButton(
                onClick = {
                    if (index >= 0) ActiveWorkout.logSet(index, DraftSet(setType, weight, reps, rpe))
                    onLogged()
                },
                diameter = 50.dp,
            )
        }
    }
}

@Composable
private fun StepperCard(
    label: String,
    value: String,
    unit: String?,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x12FFFFFF))
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = Accent, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                if (unit != null) {
                    Spacer(Modifier.width(3.dp))
                    Text(unit, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
        StepIcon("−", onMinus)
        Spacer(Modifier.width(6.dp))
        StepIcon("+", onPlus)
    }
}

@Composable
private fun StepIcon(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0x22FFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, fontSize = 20.sp, color = Accent, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun TypeChip(type: SetType, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(labelOf(type), fontSize = 13.sp, color = Accent)
    }
}

private fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

private fun nextType(t: SetType): SetType {
    val all = SetType.values()
    return all[(t.ordinal + 1) % all.size]
}

private fun labelOf(t: SetType): String = t.name.lowercase().replaceFirstChar { it.uppercase() }
