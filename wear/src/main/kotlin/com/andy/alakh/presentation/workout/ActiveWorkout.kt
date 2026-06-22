package com.andy.alakh.presentation.workout

import com.andy.alakh.shared.model.Exercise
import com.andy.alakh.shared.model.SetType
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

data class DraftExercise(
    val exercise: Exercise,
    val sets: List<DraftSet> = emptyList(),
)

data class DraftSession(
    val startedAtEpochMs: Long,
    val exercises: List<DraftExercise> = emptyList(),
)

/**
 * The in-progress workout, held in memory as a process singleton so every workout screen
 * shares one session without ViewModel plumbing.
 *
 * NOTE: persistence to the database is the next step — for now finish()/discard() just clear
 * the draft. editingIndex carries "which exercise the set-entry screen is editing" so we don't
 * need typed navigation arguments yet.
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

    /** Adds an exercise to the session and returns its index. */
    fun addExercise(exercise: Exercise): Int {
        startIfNeeded()
        val current = _draft.value!!
        _draft.value = current.copy(exercises = current.exercises + DraftExercise(exercise))
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
