package com.andy.alakh.shared.rules

import com.andy.alakh.shared.model.LoggedSet
import com.andy.alakh.shared.model.PerformedExercise
import com.andy.alakh.shared.model.SessionLog
import com.andy.alakh.shared.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrengthStatsTest {

    // --- builders -----------------------------------------------------------
    private fun set(weight: Double?, reps: Int?, completed: Boolean = true, type: SetType = SetType.NORMAL) =
        LoggedSet(setType = type, weightKg = weight, reps = reps, completed = completed)

    private fun session(id: String, exerciseId: String, vararg sets: LoggedSet) = SessionLog(
        id = id, routineId = null, name = "w", startedAtEpochMs = 0, endedAtEpochMs = 1,
        performedExercises = listOf(PerformedExercise(exerciseId = exerciseId, sets = sets.toList(), order = 0)),
    )

    // --- formula units ------------------------------------------------------
    @Test
    fun epley_matchesTheClassicFormula() {
        assertThat(StrengthStats.epleyOneRepMax(110.0, 3)).isWithin(1e-9).of(121.0)  // 110 * 1.1
        assertThat(StrengthStats.epleyOneRepMax(100.0, 5)).isWithin(1e-9).of(116.6666666667)
        assertThat(StrengthStats.epleyOneRepMax(100.0, 0)).isEqualTo(100.0)       // a single rep-less of 1RM == weight
    }

    @Test
    fun setVolume_isWeightTimesReps() {
        assertThat(StrengthStats.setVolume(60.0, 8)).isEqualTo(480.0)
    }

    // --- aggregation --------------------------------------------------------
    @Test
    fun emptyHistory_returnsEmptyStats() {
        val stats = StrengthStats.forExercise(emptyList(), "bench")
        assertThat(stats).isEqualTo(com.andy.alakh.shared.model.ExerciseStats())
        assertThat(stats.totalSets).isEqualTo(0)
        assertThat(stats.heaviestWeightKg).isNull()
    }

    @Test
    fun aggregatesAllMetricsAcrossSetsInOneSession() {
        val s = session("s1", "bench", set(100.0, 5), set(110.0, 3), set(90.0, 8))
        val stats = StrengthStats.forExercise(listOf(s), "bench")

        assertThat(stats.heaviestWeightKg).isEqualTo(110.0)
        assertThat(stats.bestSetVolumeKg).isEqualTo(720.0)            // 90 * 8
        assertThat(stats.estimatedOneRmKg!!).isWithin(1e-9).of(121.0) // 110 * 1.1 wins
        assertThat(stats.bestSessionVolumeKg).isEqualTo(1550.0)      // 500 + 330 + 720
        assertThat(stats.totalSets).isEqualTo(3)
    }

    @Test
    fun ignoresIncompleteSets() {
        val s = session("s1", "bench", set(100.0, 5), set(200.0, 1, completed = false))
        val stats = StrengthStats.forExercise(listOf(s), "bench")
        assertThat(stats.heaviestWeightKg).isEqualTo(100.0)          // the 200 isn't completed
        assertThat(stats.totalSets).isEqualTo(1)
    }

    @Test
    fun ignoresOtherExercises() {
        val bench = session("s1", "bench", set(100.0, 5))
        val squat = session("s2", "squat", set(200.0, 5))
        val stats = StrengthStats.forExercise(listOf(bench, squat), "bench")
        assertThat(stats.heaviestWeightKg).isEqualTo(100.0)
        assertThat(stats.totalSets).isEqualTo(1)
    }

    @Test
    fun bestSessionVolume_takesTheMaxAcrossSessions() {
        val s1 = session("s1", "bench", set(100.0, 5), set(110.0, 3))   // 500 + 330 = 830
        val s2 = session("s2", "bench", set(120.0, 4), set(120.0, 4))   // 480 + 480 = 960
        val stats = StrengthStats.forExercise(listOf(s1, s2), "bench")
        assertThat(stats.bestSessionVolumeKg).isEqualTo(960.0)
        assertThat(stats.heaviestWeightKg).isEqualTo(120.0)
        assertThat(stats.totalSets).isEqualTo(4)
    }

    @Test
    fun bodyweightSet_countsTowardTotalButNotWeightMetrics() {
        // A pull-up logged with reps but no weight: contributes to totalSets only.
        val s = session("s1", "pullup", set(weight = null, reps = 10))
        val stats = StrengthStats.forExercise(listOf(s), "pullup")
        assertThat(stats.totalSets).isEqualTo(1)
        assertThat(stats.heaviestWeightKg).isNull()
        assertThat(stats.bestSetVolumeKg).isNull()
        assertThat(stats.estimatedOneRmKg).isNull()
        assertThat(stats.bestSessionVolumeKg).isNull()
    }
}
