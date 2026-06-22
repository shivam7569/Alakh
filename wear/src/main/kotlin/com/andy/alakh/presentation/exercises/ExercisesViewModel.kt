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
import kotlinx.coroutines.flow.stateIn

/** A row in the catalog list: either a muscle-group section header or an exercise. */
sealed interface CatalogEntry {
    data class Header(val title: String) : CatalogEntry
    data class Item(val exercise: ExerciseListItem) : CatalogEntry
}

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

    /** Grouped sections when idle, flat filtered results when searching. Built off the main thread. */
    val entries: StateFlow<List<CatalogEntry>> =
        combine(catalog, _query) { list, q -> buildEntries(list, q) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private fun buildEntries(list: List<ExerciseListItem>, query: String): List<CatalogEntry> {
    val q = query.trim()
    if (q.isNotEmpty()) {
        return list.asSequence()
            .filter { it.name.contains(q, ignoreCase = true) }
            .sortedBy { it.name }
            .map { CatalogEntry.Item(it) }
            .toList()
    }
    val byGroup = list.groupBy { it.primaryMuscles.firstOrNull() ?: MuscleGroup.FULL_BODY }
    val entries = ArrayList<CatalogEntry>(list.size + GROUP_ORDER.size)
    for (group in GROUP_ORDER) {
        val items = byGroup[group]?.sortedBy { it.name } ?: continue
        if (items.isEmpty()) continue
        entries.add(CatalogEntry.Header(group.displayName))
        items.forEach { entries.add(CatalogEntry.Item(it)) }
    }
    return entries
}
