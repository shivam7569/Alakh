package com.andy.alakh.shared.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insert(session: WorkoutEntity): Long

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>
}
