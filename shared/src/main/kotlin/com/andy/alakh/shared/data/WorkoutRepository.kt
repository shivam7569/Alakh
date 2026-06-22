package com.andy.alakh.shared.data

import android.content.Context
import androidx.room.withTransaction
import com.andy.alakh.shared.model.ExerciseStats
import com.andy.alakh.shared.model.SessionLog

/**
 * Saves performed workouts and computes per-exercise records. Hides Room behind domain types
 * (SessionLog / ExerciseStats) so the apps never touch entities directly.
 */
class WorkoutRepository private constructor(private val db: AlakhDatabase) {

    private val dao = db.workoutDao()

    /** Persist a finished session (relationally) and bump usage for each exercise it touched. */
    suspend fun saveSession(session: SessionLog) {
        db.withTransaction {
            dao.insertSession(
                WorkoutSessionEntity(
                    id = session.id,
                    routineId = session.routineId,
                    name = session.name,
                    startedAtEpochMs = session.startedAtEpochMs,
                    endedAtEpochMs = session.endedAtEpochMs,
                ),
            )
            for (performed in session.performedExercises) {
                val performedId = dao.insertPerformed(
                    PerformedExerciseEntity(
                        sessionId = session.id,
                        exerciseId = performed.exerciseId,
                        role = performed.role,
                        restSeconds = performed.restSecondsBetweenSets,
                        orderIndex = performed.order,
                    ),
                )
                val sets = performed.sets.mapIndexed { index, set ->
                    LoggedSetEntity(
                        performedExerciseId = performedId,
                        setType = set.setType,
                        weightKg = set.weightKg,
                        reps = set.reps,
                        rpe = set.rpe,
                        durationSec = set.durationSec,
                        distanceMeters = set.distanceMeters,
                        completed = set.completed,
                        orderIndex = index,
                    )
                }
                if (sets.isNotEmpty()) dao.insertSets(sets)
            }
            session.performedExercises.map { it.exerciseId }.distinct().forEach { dao.incrementUsage(it) }
        }
    }

    suspend fun exerciseStats(exerciseId: String): ExerciseStats = ExerciseStats(
        estimatedOneRmKg = dao.estimatedOneRm(exerciseId),
        bestSessionVolumeKg = dao.bestSessionVolume(exerciseId),
        bestSetVolumeKg = dao.bestSetVolume(exerciseId),
        heaviestWeightKg = dao.heaviestWeight(exerciseId),
        totalSets = dao.totalSets(exerciseId),
    )

    companion object {
        @Volatile private var instance: WorkoutRepository? = null

        fun get(context: Context): WorkoutRepository =
            instance ?: synchronized(this) {
                instance ?: WorkoutRepository(AlakhDatabase.get(context)).also { instance = it }
            }
    }
}
