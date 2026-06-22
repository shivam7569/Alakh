package com.andy.alakh.presentation.workout

import com.andy.alakh.shared.data.ExerciseListItem
import com.andy.alakh.shared.model.ExerciseRole
import com.andy.alakh.shared.model.LoggedSet
import com.andy.alakh.shared.model.MuscleGroup
import com.andy.alakh.shared.model.PerformedExercise
import com.andy.alakh.shared.model.SessionLog
import com.andy.alakh.shared.model.SetType
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One performed set, held in memory while the workout is in progress. */
data class DraftSet(
    val setType: SetType,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
)

/** A chosen exercise + its logged sets. Stores only the light fields the UI needs (no instructions). */
data class DraftExercise(
    val exerciseId: String,
    val name: String,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val sets: List<DraftSet> = emptyList(),
)

data class DraftSession(
    val startedAtEpochMs: Long,
    val exercises: List<DraftExercise> = emptyList(),
)

/**
 * The in-progress workout, held in memory as a process singleton so every workout screen shares
 * one session. Persistence to the database is the next step — finish()/discard() just clear it.
 */
object ActiveWorkout {

    private val _draft = MutableStateFlow<DraftSession?>(null)
    val draft: StateFlow<DraftSession?> = _draft.asStateFlow()

    /** Which exercise the set-entry screen is currently logging into. */
    var editingIndex: Int = -1

    val isActive: Boolean get() = _draft.value != null

    fun startIfNeeded() {
        if (_draft.value == null) _draft.value = DraftSession(System.currentTimeMillis())
    }

    fun addExercise(item: ExerciseListItem): Int {
        startIfNeeded()
        val current = _draft.value!!
        val draftExercise = DraftExercise(
            exerciseId = item.id,
            name = item.name,
            primaryMuscles = item.primaryMuscles,
            secondaryMuscles = item.secondaryMuscles,
        )
        _draft.value = current.copy(exercises = current.exercises + draftExercise)
        return _draft.value!!.exercises.lastIndex
    }

    fun logSet(exerciseIndex: Int, set: DraftSet) {
        val current = _draft.value ?: return
        val exercise = current.exercises.getOrNull(exerciseIndex) ?: return
        val updated = current.exercises.toMutableList()
        updated[exerciseIndex] = exercise.copy(sets = exercise.sets + set)
        _draft.value = current.copy(exercises = updated)
    }

    fun exerciseAt(index: Int): DraftExercise? = _draft.value?.exercises?.getOrNull(index)

    fun lastSet(index: Int): DraftSet? = exerciseAt(index)?.sets?.lastOrNull()

    fun finish() {
        // TODO: persist the draft as a SessionLog before clearing (next step).
        _draft.value = null
        editingIndex = -1
    }

    fun discard() {
        _draft.value = null
        editingIndex = -1
    }
}

/** Converts the in-memory draft into a persistable domain SessionLog. */
fun DraftSession.toSessionLog(): SessionLog = SessionLog(
    id = UUID.randomUUID().toString(),
    routineId = null,
    name = "Workout",
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = System.currentTimeMillis(),
    performedExercises = exercises.mapIndexed { index, draft ->
        PerformedExercise(
            exerciseId = draft.exerciseId,
            role = ExerciseRole.NORMAL,
            sets = draft.sets.map { LoggedSet(it.setType, it.weightKg, it.reps, it.rpe, completed = true) },
            restSecondsBetweenSets = 90,
            order = index,
        )
    },
)
