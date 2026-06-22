package com.andy.alakh.presentation.theme

import androidx.compose.ui.graphics.Color
import com.andy.alakh.shared.model.MuscleGroup

/**
 * EXPERIMENT: a distinct color per muscle group. The exercises list paints each section (and its
 * rows) with its group color so the user can pick a favorite — whichever they like becomes the
 * app's theme accent.
 *
 * KEEP THIS MAPPING STABLE. When the user says "I like the color of <exercise>", we find that
 * exercise's first primary muscle group and read its color here.
 */
val MuscleColors: Map<MuscleGroup, Color> = mapOf(
    MuscleGroup.CHEST to Color(0xFFFF6B6B),       // coral red
    MuscleGroup.BACK to Color(0xFF4D96FF),        // blue
    MuscleGroup.SHOULDERS to Color(0xFFFFA94D),   // orange
    MuscleGroup.TRAPS to Color(0xFF38D9A9),       // teal
    MuscleGroup.BICEPS to Color(0xFF9775FA),      // purple
    MuscleGroup.TRICEPS to Color(0xFFF783AC),     // pink
    MuscleGroup.FOREARMS to Color(0xFFA9E34B),    // lime
    MuscleGroup.CORE to Color(0xFFFFD43B),        // yellow
    MuscleGroup.LOWER_BACK to Color(0xFF5C7CFA),  // indigo
    MuscleGroup.QUADS to Color(0xFF22B8CF),       // cyan
    MuscleGroup.HAMSTRINGS to Color(0xFFE64980),  // magenta
    MuscleGroup.GLUTES to Color(0xFF51CF66),      // green
    MuscleGroup.CALVES to Color(0xFFFCC419),      // amber
    MuscleGroup.SHIN to Color(0xFFB197FC),        // violet
    MuscleGroup.FULL_BODY to Color(0xFF748FFC),   // periwinkle
)

fun colorForGroup(group: MuscleGroup?): Color =
    MuscleColors[group] ?: Color(0xFF9AA3A0)
