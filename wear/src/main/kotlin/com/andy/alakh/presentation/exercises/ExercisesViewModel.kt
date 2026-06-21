package com.andy.alakh.presentation.exercises

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andy.alakh.shared.data.AlakhDatabase
import com.andy.alakh.shared.data.toModel
import com.andy.alakh.shared.model.Exercise
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Streams the exercise catalog from the local database to the UI. */
class ExercisesViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AlakhDatabase.get(app).exerciseDao()

    val exercises: StateFlow<List<Exercise>> =
        dao.observeAll()
            .map { rows -> rows.map { it.toModel() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
