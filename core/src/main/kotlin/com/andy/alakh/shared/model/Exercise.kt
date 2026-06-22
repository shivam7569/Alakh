package com.andy.alakh.shared.model

/** Broad type of movement. Lets us cover strength AND cardio/Hyrox/mobility in one catalog. */
enum class ExerciseCategory { STRENGTH, OLYMPIC, BODYWEIGHT, CARDIO, FUNCTIONAL, MOBILITY, STRETCHING }

enum class Equipment {
    BARBELL, DUMBBELL, KETTLEBELL, MACHINE, CABLE, BANDS, BODYWEIGHT,
    MEDICINE_BALL, SLED, SANDBAG, CARDIO_MACHINE, OTHER,
}

/** What we ask the user to log for this exercise — drives which inputs the watch shows. */
enum class TrackedMetric {
    WEIGHT_AND_REPS,    // most strength work
    REPS_ONLY,          // bodyweight reps (push-ups, pull-ups)
    TIME,               // planks, carries / holds for time
    DISTANCE,           // runs, rows, ski-erg
    TIME_AND_DISTANCE,  // cardio with both
}

/**
 * A catalog exercise. Seeded from free-exercise-db (public domain) for strength/bodyweight
 * movements, with cardio/Hyrox/functional entries hand-authored in the same shape, plus any
 * user-created custom exercises (isCustom = true).
 */
data class Exercise(
    val id: String,                         // stable slug (dataset) or generated id for custom
    val name: String,
    val category: ExerciseCategory,
    val equipment: Equipment,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val trackedMetric: TrackedMetric = TrackedMetric.WEIGHT_AND_REPS,
    val instructions: List<String> = emptyList(),
    val isCustom: Boolean = false,
)
