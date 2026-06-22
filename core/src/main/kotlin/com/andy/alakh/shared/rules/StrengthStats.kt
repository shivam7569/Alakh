package com.andy.alakh.shared.rules

import com.andy.alakh.shared.model.ExerciseStats
import com.andy.alakh.shared.model.SessionLog

/**
 * Pure, framework-free computation of per-exercise personal records (requirement #7) from logged
 * history. This is the single source of truth for the metric formulas; the SQL in
 * `WorkoutDao` (the on-device DB path, optimised for large history) deliberately mirrors it.
 * Because this is plain Kotlin it can be unit-tested on CI and reused by the phone's analysis
 * layer without re-querying the database.
 *
 * A set only counts when it's `completed`. Weight/volume metrics additionally need both weight
 * and reps (a plank or a bodyweight set contributes to `totalSets` but not to 1RM/volume).
 */
object StrengthStats {

    /** Epley estimate of a one-rep max from a single set. */
    fun epleyOneRepMax(weightKg: Double, reps: Int): Double = weightKg * (1.0 + reps / 30.0)

    /** Volume load of a single set (kg). */
    fun setVolume(weightKg: Double, reps: Int): Double = weightKg * reps

    /** Aggregate every logged set of [exerciseId] across [sessions] into headline records. */
    fun forExercise(sessions: List<SessionLog>, exerciseId: String): ExerciseStats {
        // Each completed set of this exercise, tagged with the session it belongs to.
        val completed = sessions.flatMap { session ->
            session.performedExercises
                .filter { it.exerciseId == exerciseId }
                .flatMap { performed -> performed.sets.filter { it.completed }.map { session.id to it } }
        }
        if (completed.isEmpty()) return ExerciseStats()

        // Weight metrics only consider sets that have BOTH weight and reps.
        val weighted = completed.filter { (_, set) -> set.weightKg != null && set.reps != null }

        val heaviest = completed.mapNotNull { (_, set) -> set.weightKg }.maxOrNull()
        val bestSet = weighted.map { (_, set) -> setVolume(set.weightKg!!, set.reps!!) }.maxOrNull()
        val oneRm = weighted.map { (_, set) -> epleyOneRepMax(set.weightKg!!, set.reps!!) }.maxOrNull()
        val bestSession = weighted
            .groupBy({ (sessionId, _) -> sessionId }, { (_, set) -> setVolume(set.weightKg!!, set.reps!!) })
            .mapValues { (_, volumes) -> volumes.sum() }
            .values.maxOrNull()

        return ExerciseStats(
            estimatedOneRmKg = oneRm,
            bestSessionVolumeKg = bestSession,
            bestSetVolumeKg = bestSet,
            heaviestWeightKg = heaviest,
            totalSets = completed.size,
        )
    }
}
