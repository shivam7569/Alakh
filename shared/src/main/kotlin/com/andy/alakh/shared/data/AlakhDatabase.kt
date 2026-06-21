package com.andy.alakh.shared.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * On-device workout history. Not yet wired into the workout flow — persist a
 * WorkoutEntity from ExerciseService.stop() when you're ready to keep history,
 * and read observeAll() on a future history screen (or the phone app).
 */
@Database(entities = [WorkoutEntity::class], version = 1, exportSchema = false)
abstract class AlakhDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var instance: AlakhDatabase? = null

        fun get(context: Context): AlakhDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlakhDatabase::class.java,
                    "alakh.db",
                ).build().also { instance = it }
            }
    }
}
