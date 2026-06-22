package com.andy.alakh.shared.model

/**
 * A performed workout: logged on the WATCH (standalone, mid-workout) and synced back to the
 * PHONE for analysis. This is the "watch logs → phone analyzes" half of the architecture.
 */
data class SessionLog(
    val id: String,
    val routineId: String?,            // null = freestyle / ad-hoc session
    val name: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,         // null while the session is in progress
    val performedExercises: List<PerformedExercise>,
)

data class PerformedExercise(
    val exerciseId: String,
    val role: ExerciseRole = ExerciseRole.NORMAL,
    val sets: List<LoggedSet>,
    val restSecondsBetweenSets: Int = 90,
    val order: Int,
)
