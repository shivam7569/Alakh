package com.andy.alakh.shared.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    /** Used to seed the catalog and to add/update custom exercises. REPLACE = upsert by id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    /** Lightweight projection for lists/pickers — skips heavy columns (instructions, etc.). */
    @Query("SELECT id, name, category, primaryMuscles, secondaryMuscles FROM exercises ORDER BY name")
    fun observeListItems(): Flow<List<ExerciseListItem>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun search(query: String): Flow<List<ExerciseEntity>>

    /** Lets the app decide whether the catalog still needs seeding on first launch. */
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}
