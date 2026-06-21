package com.andy.alakh.shared.model

/**
 * Framework-free domain types shared by the watch app and the (future) phone app.
 * Nothing here depends on Health Services or Android UI, so both modules can reuse it.
 */

/** A type of workout the user can start. The wear module maps this to a Health Services ExerciseType. */
enum class WorkoutType {
    WALKING,
    RUNNING,
    OUTDOOR_CYCLING,
    HIKING,
    OTHER,
}

/** High-level state of an exercise session, independent of the Health Services SDK. */
enum class ExerciseStatus {
    NOT_STARTED,
    PREPARING,
    ACTIVE,
    PAUSED,
    ENDED,
}

/** Live metrics emitted during an active exercise. A null field = not available yet / unsupported. */
data class ExerciseMetrics(
    val heartRateBpm: Int? = null,
    val calories: Double? = null,
    val distanceMeters: Double? = null,
    val elapsedMs: Long = 0L,
)

/** A completed session, suitable for history/summary display. */
data class WorkoutSession(
    val type: WorkoutType,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val avgHeartRateBpm: Int?,
    val calories: Double?,
    val distanceMeters: Double?,
)
