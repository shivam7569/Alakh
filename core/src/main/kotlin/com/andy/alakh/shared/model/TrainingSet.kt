package com.andy.alakh.shared.model

import kotlinx.serialization.Serializable

/** How a single set counts. (Requirement: normal / warm-up / failure / drop.) */
@Serializable
enum class SetType { NORMAL, WARMUP, FAILURE, DROP }

/** Whether an exercise within a session is real work or a warm-up movement. (Requirement.) */
@Serializable
enum class ExerciseRole { NORMAL, WARMUP }

/**
 * One performed set. Fields are nullable because what matters depends on the exercise's
 * TrackedMetric — a plank has a duration but no weight; a back squat has weight + reps.
 */
@Serializable
data class LoggedSet(
    val setType: SetType = SetType.NORMAL,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rpe: Double? = null,            // 1.0–10.0 in 0.5 steps (Rate of Perceived Exertion)
    val durationSec: Int? = null,       // for TIME / TIME_AND_DISTANCE
    val distanceMeters: Double? = null, // for DISTANCE / TIME_AND_DISTANCE
    val completed: Boolean = false,
) {
    /** Volume load (kg) for strength sets — the basis of per-muscle volume + spider weighting. */
    val volumeLoad: Double
        get() = if (weightKg != null && reps != null) weightKg * reps else 0.0
}

/** A planned target set from a routine, used to pre-fill the watch logger ("accept or nudge"). */
@Serializable
data class PlannedSet(
    val setType: SetType = SetType.NORMAL,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val targetRpe: Double? = null,
)
