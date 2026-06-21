package com.andy.alakh.shared.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andy.alakh.shared.model.Equipment
import com.andy.alakh.shared.model.Exercise
import com.andy.alakh.shared.model.ExerciseCategory
import com.andy.alakh.shared.model.MuscleGroup
import com.andy.alakh.shared.model.TrackedMetric

/** Database row for one catalog exercise. Mirrors the domain [Exercise] one-to-one. */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: ExerciseCategory,
    val equipment: Equipment,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val trackedMetric: TrackedMetric,
    val instructions: List<String>,
    val isCustom: Boolean,
)

fun ExerciseEntity.toModel(): Exercise = Exercise(
    id = id,
    name = name,
    category = category,
    equipment = equipment,
    primaryMuscles = primaryMuscles,
    secondaryMuscles = secondaryMuscles,
    trackedMetric = trackedMetric,
    instructions = instructions,
    isCustom = isCustom,
)

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    category = category,
    equipment = equipment,
    primaryMuscles = primaryMuscles,
    secondaryMuscles = secondaryMuscles,
    trackedMetric = trackedMetric,
    instructions = instructions,
    isCustom = isCustom,
)
