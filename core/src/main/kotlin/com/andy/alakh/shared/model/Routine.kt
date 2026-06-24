package com.andy.alakh.shared.model

import kotlinx.serialization.Serializable

/**
 * A reusable plan authored on the PHONE and synced to the WATCH to drive a session.
 * This is the "phone plans → watch logs" half of the architecture. @Serializable so it can travel
 * over the Wear Data Layer as JSON (see com.andy.alakh.shared.sync.RoutineCodec).
 */
@Serializable
data class Routine(
    val id: String,
    val name: String,
    val exercises: List<RoutineExercise>,
)

@Serializable
data class RoutineExercise(
    val exerciseId: String,
    val role: ExerciseRole = ExerciseRole.NORMAL,
    val plannedSets: List<PlannedSet>,
    val restSecondsBetweenSets: Int = 90,   // rest periods (requirement #5)
    val order: Int,
)
