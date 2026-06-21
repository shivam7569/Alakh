package com.andy.alakh.shared.model

/**
 * A reusable plan authored on the PHONE and synced to the WATCH to drive a session.
 * This is the "phone plans → watch logs" half of the architecture.
 */
data class Routine(
    val id: String,
    val name: String,
    val exercises: List<RoutineExercise>,
)

data class RoutineExercise(
    val exerciseId: String,
    val role: ExerciseRole = ExerciseRole.NORMAL,
    val plannedSets: List<PlannedSet>,
    val restSecondsBetweenSets: Int = 90,   // rest periods (requirement #5)
    val order: Int,
)
