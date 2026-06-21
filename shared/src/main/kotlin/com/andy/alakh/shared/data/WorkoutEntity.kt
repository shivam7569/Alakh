package com.andy.alakh.shared.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row for a persisted workout session. */
@Entity(tableName = "workout_sessions")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val avgHeartRateBpm: Int?,
    val calories: Double?,
    val distanceMeters: Double?,
)
