package com.andy.alakh.presentation.theme

import androidx.compose.ui.graphics.Color
import com.andy.alakh.shared.model.MuscleGroup

/**
 * The app's theme accent — the original green (the user preferred it over the cyan experiment).
 * Applied as a FONT / highlight color only (headers, row text, heatmap, tick, breathing) — NOT to
 * component backgrounds (we deliberately don't override Material primary).
 */
val AlakhAccent = Color(0xFF34C796)

/**
 * Per-muscle-group palette (from the color experiment). No longer used to color the catalog list
 * (we picked a single theme accent), but kept for future per-muscle visuals (e.g. the spider chart).
 */
val MuscleColors: Map<MuscleGroup, Color> = mapOf(
    MuscleGroup.CHEST to Color(0xFFFF6B6B),
    MuscleGroup.BACK to Color(0xFF4D96FF),
    MuscleGroup.SHOULDERS to Color(0xFFFFA94D),
    MuscleGroup.TRAPS to Color(0xFF38D9A9),
    MuscleGroup.BICEPS to Color(0xFF9775FA),
    MuscleGroup.TRICEPS to Color(0xFFF783AC),
    MuscleGroup.FOREARMS to Color(0xFFA9E34B),
    MuscleGroup.CORE to Color(0xFFFFD43B),
    MuscleGroup.LOWER_BACK to Color(0xFF5C7CFA),
    MuscleGroup.QUADS to Color(0xFF22B8CF),
    MuscleGroup.HAMSTRINGS to Color(0xFFE64980),
    MuscleGroup.GLUTES to Color(0xFF51CF66),
    MuscleGroup.CALVES to Color(0xFFFCC419),
    MuscleGroup.SHIN to Color(0xFFB197FC),
    MuscleGroup.FULL_BODY to Color(0xFF748FFC),
)

fun colorForGroup(group: MuscleGroup?): Color = MuscleColors[group] ?: AlakhAccent
