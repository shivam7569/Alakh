package com.andy.alakh.shared.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The app's local database, shared by the watch and phone modules.
 *
 * Currently holds the exercise catalog; session/routine/breathing/readiness tables get added
 * (with a version bump) when we build the logging UI. While the schema is still evolving we use
 * fallbackToDestructiveMigration — if a table's shape changes, Room wipes and rebuilds the DB
 * instead of crashing. That's fine now (no real data yet); before release we'll write proper
 * migrations so history is preserved.
 */
@Database(
    entities = [ExerciseEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AlakhDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile private var instance: AlakhDatabase? = null

        fun get(context: Context): AlakhDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlakhDatabase::class.java,
                    "alakh.db",
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
