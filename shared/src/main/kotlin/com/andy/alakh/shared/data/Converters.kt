package com.andy.alakh.shared.data

import androidx.room.TypeConverter
import com.andy.alakh.shared.model.Equipment
import com.andy.alakh.shared.model.ExerciseCategory
import com.andy.alakh.shared.model.MuscleGroup
import com.andy.alakh.shared.model.TrackedMetric

/**
 * Room stores only primitive columns (text, numbers). These converters translate our enums
 * and lists to/from plain strings so they can live in a single table row. Registered on the
 * database via @TypeConverters(Converters::class).
 */
class Converters {

    @TypeConverter fun categoryToString(v: ExerciseCategory): String = v.name
    @TypeConverter fun stringToCategory(v: String): ExerciseCategory = ExerciseCategory.valueOf(v)

    @TypeConverter fun equipmentToString(v: Equipment): String = v.name
    @TypeConverter fun stringToEquipment(v: String): Equipment = Equipment.valueOf(v)

    @TypeConverter fun metricToString(v: TrackedMetric): String = v.name
    @TypeConverter fun stringToMetric(v: String): TrackedMetric = TrackedMetric.valueOf(v)

    @TypeConverter
    fun musclesToString(v: List<MuscleGroup>): String = v.joinToString(",") { it.name }

    @TypeConverter
    fun stringToMuscles(v: String): List<MuscleGroup> =
        if (v.isBlank()) emptyList() else v.split(",").map { MuscleGroup.valueOf(it) }

    // Instructions are full sentences (may contain commas), so join on newline.
    @TypeConverter
    fun instructionsToString(v: List<String>): String = v.joinToString("\n")

    @TypeConverter
    fun stringToInstructions(v: String): List<String> =
        if (v.isBlank()) emptyList() else v.split("\n")
}
