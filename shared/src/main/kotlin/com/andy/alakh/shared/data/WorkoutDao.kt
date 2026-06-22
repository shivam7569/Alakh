package com.andy.alakh.shared.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkoutDao {

    @Insert suspend fun insertSession(session: WorkoutSessionEntity)

    /** Returns the auto-generated row id so its sets can reference it. */
    @Insert suspend fun insertPerformed(performed: PerformedExerciseEntity): Long

    @Insert suspend fun insertSets(sets: List<LoggedSetEntity>)

    /** Bumps the catalog usage counter so most-used exercises can sort to the top. */
    @Query("UPDATE exercises SET usageCount = usageCount + 1 WHERE id = :exerciseId")
    suspend fun incrementUsage(exerciseId: String)

    @Query(
        """
        SELECT MAX(ls.weightKg) FROM logged_sets ls
        JOIN performed_exercises pe ON ls.performedExerciseId = pe.id
        WHERE pe.exerciseId = :exerciseId AND ls.completed = 1
        """,
    )
    suspend fun heaviestWeight(exerciseId: String): Double?

    @Query(
        """
        SELECT MAX(ls.weightKg * ls.reps) FROM logged_sets ls
        JOIN performed_exercises pe ON ls.performedExerciseId = pe.id
        WHERE pe.exerciseId = :exerciseId AND ls.completed = 1
          AND ls.weightKg IS NOT NULL AND ls.reps IS NOT NULL
        """,
    )
    suspend fun bestSetVolume(exerciseId: String): Double?

    @Query(
        """
        SELECT MAX(ls.weightKg * (1 + ls.reps / 30.0)) FROM logged_sets ls
        JOIN performed_exercises pe ON ls.performedExerciseId = pe.id
        WHERE pe.exerciseId = :exerciseId AND ls.completed = 1
          AND ls.weightKg IS NOT NULL AND ls.reps IS NOT NULL
        """,
    )
    suspend fun estimatedOneRm(exerciseId: String): Double?

    @Query(
        """
        SELECT MAX(sv) FROM (
          SELECT SUM(ls.weightKg * ls.reps) AS sv FROM logged_sets ls
          JOIN performed_exercises pe ON ls.performedExerciseId = pe.id
          WHERE pe.exerciseId = :exerciseId AND ls.completed = 1
            AND ls.weightKg IS NOT NULL AND ls.reps IS NOT NULL
          GROUP BY pe.sessionId
        )
        """,
    )
    suspend fun bestSessionVolume(exerciseId: String): Double?

    @Query(
        """
        SELECT COUNT(*) FROM logged_sets ls
        JOIN performed_exercises pe ON ls.performedExerciseId = pe.id
        WHERE pe.exerciseId = :exerciseId AND ls.completed = 1
        """,
    )
    suspend fun totalSets(exerciseId: String): Int
}
