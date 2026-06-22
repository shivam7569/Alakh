package com.andy.alakh.shared.data

import com.andy.alakh.shared.model.ExerciseCategory
import com.andy.alakh.shared.model.MuscleGroup

/**
 * Lightweight row for lists and pickers. Deliberately omits the heavy fields (instructions,
 * equipment, tracked metric) so browsing 800+ exercises stays cheap on the watch. Room maps the
 * projection query straight into this — no main-thread conversion of full entities.
 */
data class ExerciseListItem(
    val id: String,
    val name: String,
    val category: ExerciseCategory,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val usageCount: Int = 0,
)
