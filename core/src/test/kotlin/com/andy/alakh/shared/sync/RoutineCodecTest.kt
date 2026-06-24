package com.andy.alakh.shared.sync

import com.andy.alakh.shared.model.ExerciseRole
import com.andy.alakh.shared.model.PlannedSet
import com.andy.alakh.shared.model.Routine
import com.andy.alakh.shared.model.RoutineExercise
import com.andy.alakh.shared.model.SetType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoutineCodecTest {

    private val routine = Routine(
        id = "push-day",
        name = "Push Day",
        exercises = listOf(
            RoutineExercise(
                exerciseId = "bench",
                role = ExerciseRole.NORMAL,
                plannedSets = listOf(
                    PlannedSet(SetType.WARMUP, targetReps = 10, targetWeightKg = 20.0),
                    PlannedSet(SetType.NORMAL, targetReps = 8, targetWeightKg = 60.0, targetRpe = 8.0),
                ),
                restSecondsBetweenSets = 120,
                order = 0,
            ),
            RoutineExercise(
                exerciseId = "ohp",
                plannedSets = listOf(PlannedSet(targetReps = 5)), // nulls for weight/rpe + defaults
                order = 1,
            ),
        ),
    )

    @Test
    fun roundTripsExactly() {
        assertThat(RoutineCodec.decode(RoutineCodec.encode(routine))).isEqualTo(routine)
    }

    @Test
    fun encodesIdsAndNames() {
        val json = RoutineCodec.encode(routine)
        assertThat(json).contains("push-day")
        assertThat(json).contains("bench")
        assertThat(json).contains("ohp")
    }

    @Test
    fun decode_toleratesUnknownFields() {
        // Forward-compatible: a newer app version could add fields; an older decoder must not crash.
        val json = """{"id":"r","name":"R","exercises":[],"futureField":42}"""
        val r = RoutineCodec.decode(json)
        assertThat(r.id).isEqualTo("r")
        assertThat(r.name).isEqualTo("R")
        assertThat(r.exercises).isEmpty()
    }
}
