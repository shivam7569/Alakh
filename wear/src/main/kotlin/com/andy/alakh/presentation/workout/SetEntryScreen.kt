package com.andy.alakh.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.PickerState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import com.andy.alakh.presentation.components.TickButton
import com.andy.alakh.shared.model.SetType
import kotlin.math.roundToInt

private val Accent = Color(0xFF34C796)
private val Muted = Color(0xFF9AA3A0)

private const val WEIGHT_STEP = 2.5
private const val MAX_WEIGHT_INDEX = 160 // 0..400 kg in 2.5 steps
private const val MAX_REPS_INDEX = 60

/**
 * Interactive set logging: weight + reps are crown/touch-scrollable wheels (centre value is the
 * selection, neighbours fade), pre-set from the previous set. RPE + set type are compact, and the
 * tick logs the set.
 */
@Composable
fun SetEntryScreen() {
    val index = ActiveWorkout.editingIndex
    val draftExercise = ActiveWorkout.exerciseAt(index)
    val last = ActiveWorkout.lastSet(index)

    val weightState = rememberPickerState(
        initialNumberOfOptions = MAX_WEIGHT_INDEX + 1,
        initiallySelectedIndex = ((last?.weightKg ?: 20.0) / WEIGHT_STEP).roundToInt().coerceIn(0, MAX_WEIGHT_INDEX),
    )
    val repsState = rememberPickerState(
        initialNumberOfOptions = MAX_REPS_INDEX + 1,
        initiallySelectedIndex = (last?.reps ?: 8).coerceIn(0, MAX_REPS_INDEX),
    )
    var rpe by remember { mutableStateOf(last?.rpe ?: 8.0) }
    var setType by remember { mutableStateOf(last?.setType ?: SetType.NORMAL) }
    var loggedCount by remember { mutableIntStateOf(0) }
    var lastLogged by remember { mutableStateOf(last) }

    val weightFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { weightFocus.requestFocus() } }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 10.dp, end = 10.dp, top = 26.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = draftExercise?.name ?: "Set",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (loggedCount > 0) {
                Text(
                    "✓ $loggedCount logged · ${fmt(lastLogged?.weightKg)} kg × ${lastLogged?.reps ?: 0}",
                    fontSize = 11.sp,
                    color = Accent,
                )
            } else if (last != null) {
                Text("prev  ${fmt(last.weightKg)} kg × ${last.reps ?: 0}", fontSize = 11.sp, color = Muted)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                WheelColumn("KG", weightState, weightFocus) { fmt(it * WEIGHT_STEP) }
                WheelColumn("REPS", repsState, null) { it.toString() }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RpeControl(rpe, { rpe = (rpe - 0.5).coerceAtLeast(1.0) }, { rpe = (rpe + 0.5).coerceAtMost(10.0) })
                TypeChip(setType) { setType = nextType(setType) }
            }

            Spacer(Modifier.height(2.dp))
            // Tick logs the set and STAYS (pre-filled for the next set) — log set after set without
            // re-opening the exercise. Swipe back when done with this exercise.
            TickButton(
                onClick = {
                    if (index >= 0) {
                        val set = DraftSet(
                            setType = setType,
                            weightKg = weightState.selectedOptionIndex * WEIGHT_STEP,
                            reps = repsState.selectedOptionIndex,
                            rpe = rpe,
                        )
                        ActiveWorkout.logSet(index, set)
                        RestTimer.start() // auto-start the rest countdown after logging a set
                        lastLogged = set
                        loggedCount++
                    }
                },
                diameter = 50.dp,
            )
        }
    }
}

@Composable
private fun WheelColumn(
    label: String,
    state: PickerState,
    focusRequester: FocusRequester?,
    format: (Int) -> String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Accent, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Picker(
            state = state,
            contentDescription = { label },
            modifier = (focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier).size(width = 66.dp, height = 92.dp),
        ) { optionIndex ->
            val selected = optionIndex == state.selectedOptionIndex
            Text(
                text = format(optionIndex),
                fontSize = if (selected) 26.sp else 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else Muted,
            )
        }
    }
}

@Composable
private fun RpeControl(value: Double, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        StepIcon("−", onMinus)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text("RPE", fontSize = 9.sp, color = Accent)
            Text(fmt(value), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        StepIcon("+", onPlus)
    }
}

@Composable
private fun StepIcon(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(0x22FFFFFF)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, fontSize = 18.sp, color = Accent, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun TypeChip(type: SetType, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(labelOf(type), fontSize = 12.sp, color = Accent)
    }
}

private fun fmt(d: Double?): String {
    val v = d ?: 0.0
    return if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
}

private fun nextType(t: SetType): SetType {
    val all = SetType.values()
    return all[(t.ordinal + 1) % all.size]
}

private fun labelOf(t: SetType): String = t.name.lowercase().replaceFirstChar { it.uppercase() }
