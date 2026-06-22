package com.andy.alakh.shared.rules

import com.andy.alakh.shared.model.Equipment
import com.andy.alakh.shared.model.Exercise
import com.andy.alakh.shared.model.ExerciseCategory
import com.andy.alakh.shared.model.LoggedSet
import com.andy.alakh.shared.model.MuscleGroup
import com.andy.alakh.shared.model.PerformedExercise
import com.andy.alakh.shared.model.SessionLog
import com.andy.alakh.shared.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MuscleDistributionTest {

    private val bench = Exercise(
        id = "bench", name = "Bench Press", category = ExerciseCategory.STRENGTH, equipment = Equipment.BARBELL,
        primaryMuscles = listOf(MuscleGroup.CHEST), secondaryMuscles = listOf(MuscleGroup.TRICEPS),
    )
    private val squat = Exercise(
        id = "squat", name = "Back Squat", category = ExerciseCategory.STRENGTH, equipment = Equipment.BARBELL,
        primaryMuscles = listOf(MuscleGroup.QUADS), secondaryMuscles = listOf(MuscleGroup.GLUTES),
    )
    private val lookup: (String) -> Exercise? = { id ->
        when (id) { "bench" -> bench; "squat" -> squat; else -> null }
    }

    private fun workingSet() = LoggedSet(SetType.NORMAL, weightKg = 60.0, reps = 8, rpe = 8.0, completed = true)
    private fun warmupSet() = LoggedSet(SetType.WARMUP, weightKg = 20.0, reps = 10, rpe = 4.0, completed = true)

    private fun session(vararg performed: PerformedExercise) =
        SessionLog(id = "s", routineId = null, name = "w", startedAtEpochMs = 0, endedAtEpochMs = 1, performedExercises = performed.toList())

    @Test
    fun rawScores_weightPrimaryFullSecondaryHalf_perWorkingSet() {
        val s = session(
            PerformedExercise("bench", sets = listOf(workingSet(), workingSet(), workingSet()), order = 0),
            PerformedExercise("squat", sets = listOf(workingSet(), workingSet(), warmupSet()), order = 1),
        )
        val raw = MuscleDistribution.rawScores(s, lookup)
        assertThat(raw[MuscleGroup.CHEST]).isEqualTo(3.0)    // primary 1.0 x 3 working sets
        assertThat(raw[MuscleGroup.TRICEPS]).isEqualTo(1.5)  // secondary 0.5 x 3
        assertThat(raw[MuscleGroup.QUADS]).isEqualTo(2.0)    // warm-up excluded -> 2 working sets
        assertThat(raw[MuscleGroup.GLUTES]).isEqualTo(1.0)
    }

    @Test
    fun normalizedPercent_scalesToTheMostWorkedGroup() {
        val s = session(
            PerformedExercise("bench", sets = listOf(workingSet(), workingSet(), workingSet()), order = 0),
            PerformedExercise("squat", sets = listOf(workingSet(), workingSet()), order = 1),
        )
        val pct = MuscleDistribution.normalizedPercent(s, lookup)
        assertThat(pct[MuscleGroup.CHEST]).isEqualTo(100)   // most worked
        assertThat(pct[MuscleGroup.QUADS]).isEqualTo(66)    // 2.0 / 3.0
        assertThat(pct[MuscleGroup.TRICEPS]).isEqualTo(50)  // 1.5 / 3.0
    }

    @Test
    fun unknownExerciseIsSkipped() {
        val s = session(PerformedExercise("ghost", sets = listOf(workingSet()), order = 0))
        assertThat(MuscleDistribution.rawScores(s, lookup)).isEmpty()
    }

    @Test
    fun emptySession_normalizesToEmpty() {
        assertThat(MuscleDistribution.normalizedPercent(session(), lookup)).isEmpty()
    }
}
