package com.andy.alakh.shared.model

/**
 * Per-exercise personal records, computed from logged history. All weights in kg.
 * Nulls / zero = no qualifying history yet. Historical charts live on the phone; the watch shows
 * just these headline numbers.
 */
data class ExerciseStats(
    val estimatedOneRmKg: Double? = null,   // Epley: weight x (1 + reps/30), best across all sets
    val bestSessionVolumeKg: Double? = null, // best single-session total (sum of weight x reps)
    val bestSetVolumeKg: Double? = null,     // best single set (weight x reps)
    val heaviestWeightKg: Double? = null,    // top weight ever moved
    val totalSets: Int = 0,
)
