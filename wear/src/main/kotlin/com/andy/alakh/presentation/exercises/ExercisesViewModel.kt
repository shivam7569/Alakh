package com.andy.alakh.presentation.exercises

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andy.alakh.shared.data.AlakhDatabase
import com.andy.alakh.shared.data.ExerciseListItem
import com.andy.alakh.shared.model.MuscleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A collapsible muscle-group section: its group + the exercises in it. */
data class GroupSection(val group: MuscleGroup, val items: List<ExerciseListItem>)

private val GROUP_ORDER = listOf(
    MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.TRAPS,
    MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS, MuscleGroup.CORE,
    MuscleGroup.LOWER_BACK, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
    MuscleGroup.CALVES, MuscleGroup.SHIN, MuscleGroup.FULL_BODY,
)

class ExercisesViewModel(app: Application) : AndroidViewModel(app) {

    private val catalog = AlakhDatabase.get(app).exerciseDao().observeListItems()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(value: String) { _query.value = value }

    /** Muscle-group sections (for the collapsible browse view). Built off the main thread. */
    val sections: StateFlow<List<GroupSection>> =
        catalog.map { groupIntoSections(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Flat search results (empty when the query is blank). Built off the main thread. */
    val searchResults: StateFlow<List<ExerciseListItem>> =
        combine(catalog, _query) { list, raw ->
            val q = raw.trim()
            if (q.isEmpty()) emptyList()
            else list.asSequence().filter { matches(it, q) }.sortedBy { it.name }.toList()
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private fun groupIntoSections(list: List<ExerciseListItem>): List<GroupSection> {
    val byGroup = list.groupBy { it.primaryMuscles.firstOrNull() ?: MuscleGroup.FULL_BODY }
    return GROUP_ORDER.mapNotNull { group ->
        val items = byGroup[group]?.takeIf { it.isNotEmpty() }?.sortedBy { it.name }
        items?.let { GroupSection(group, it) }
    }
}

/**
 * Match by name, category, OR PRIMARY muscle group. Secondary muscles are excluded: back/core are
 * secondary in a huge share of exercises, which made "back" match almost everything.
 */
private fun matches(item: ExerciseListItem, q: String): Boolean {
    if (item.name.contains(q, ignoreCase = true)) return true
    if (item.category.name.contains(q, ignoreCase = true)) return true
    return item.primaryMuscles.any { it.displayName.contains(q, ignoreCase = true) }
}
