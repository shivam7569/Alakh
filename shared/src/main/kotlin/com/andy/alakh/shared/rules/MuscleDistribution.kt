package com.andy.alakh.shared.rules

import com.andy.alakh.shared.model.Exercise
import com.andy.alakh.shared.model.MuscleGroup
import com.andy.alakh.shared.model.SessionLog
import com.andy.alakh.shared.model.SetType

/**
 * Computes "percentage of each muscle group targeted" for the post-workout spider chart
 * (requirement #8). A primary muscle counts full, a secondary muscle counts half, scaled
 * by how many completed *working* sets that exercise got (warm-up sets are ignored so they
 * don't distort the picture).
 *
 * Pure and framework-free → easy to unit-test and reuse on both watch and phone.
 */
object MuscleDistribution {

    private const val PRIMARY_WEIGHT = 1.0
    private const val SECONDARY_WEIGHT = 0.5

    /** Raw per-muscle scores (not yet normalized). `exerciseLookup` maps an exerciseId to its catalog entry. */
    fun rawScores(
        session: SessionLog,
        exerciseLookup: (String) -> Exercise?,
    ): Map<MuscleGroup, Double> {
        val scores = mutableMapOf<MuscleGroup, Double>()
        for (performed in session.performedExercises) {
            val exercise = exerciseLookup(performed.exerciseId) ?: continue
            val workingSets = performed.sets.count { it.completed && it.setType != SetType.WARMUP }
            if (workingSets == 0) continue
            exercise.primaryMuscles.forEach { m ->
                scores[m] = (scores[m] ?: 0.0) + PRIMARY_WEIGHT * workingSets
            }
            exercise.secondaryMuscles.forEach { m ->
                scores[m] = (scores[m] ?: 0.0) + SECONDARY_WEIGHT * workingSets
            }
        }
        return scores
    }

    /**
     * Normalized 0–100 per muscle group, relative to the most-worked group in the session,
     * limited to radar axes. This is what the spider chart plots.
     */
    fun normalizedPercent(
        session: SessionLog,
        exerciseLookup: (String) -> Exercise?,
    ): Map<MuscleGroup, Int> {
        val raw = rawScores(session, exerciseLookup).filterKeys { it.onRadar }
        val max = raw.values.maxOrNull() ?: return emptyMap()
        if (max <= 0.0) return emptyMap()
        return raw.mapValues { (_, v) -> ((v / max) * 100).toInt() }
    }
}
