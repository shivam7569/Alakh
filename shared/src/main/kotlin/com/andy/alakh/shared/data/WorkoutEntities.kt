package com.andy.alakh.shared.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andy.alakh.shared.model.ExerciseRole
import com.andy.alakh.shared.model.SetType

/** A performed workout. Children cascade-delete with it. */
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val routineId: String?,
    val name: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
)

@Entity(
    tableName = "performed_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class PerformedExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val exerciseId: String,
    val role: ExerciseRole,
    val restSeconds: Int,
    val orderIndex: Int,
)

@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = PerformedExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["performedExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("performedExerciseId")],
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val performedExerciseId: Long,
    val setType: SetType,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val durationSec: Int?,
    val distanceMeters: Double?,
    val completed: Boolean,
    val orderIndex: Int,
)
