package com.andy.alakh.presentation.exercises

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andy.alakh.shared.data.AlakhDatabase
import com.andy.alakh.shared.data.ExerciseListItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Streams the catalog as lightweight list items. The Room projection query runs on Room's
 * background executor, so the watch's main thread never converts 800+ full records.
 */
class ExercisesViewModel(app: Application) : AndroidViewModel(app) {

    val exercises: StateFlow<List<ExerciseListItem>> =
        AlakhDatabase.get(app).exerciseDao().observeListItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
