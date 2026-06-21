package com.andy.alakh.shared.data

import android.content.Context
import com.andy.alakh.shared.model.Equipment
import com.andy.alakh.shared.model.ExerciseCategory
import com.andy.alakh.shared.model.MuscleGroup
import com.andy.alakh.shared.model.TrackedMetric
import org.json.JSONArray
import org.json.JSONObject

/**
 * One-time loader: on first launch (empty table) it reads the bundled catalog asset
 * (exercises_seed.json) and inserts it into the database. Uses Android's built-in org.json
 * so it needs no extra dependency. Each row is parsed defensively — a malformed entry is
 * skipped rather than failing the whole seed.
 */
object CatalogSeeder {

    private const val ASSET = "exercises_seed.json"

    suspend fun seedIfEmpty(context: Context, dao: ExerciseDao) {
        if (dao.count() > 0) return
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        val entities = ArrayList<ExerciseEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            runCatching {
                ExerciseEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    category = ExerciseCategory.valueOf(o.getString("category")),
                    equipment = Equipment.valueOf(o.getString("equipment")),
                    primaryMuscles = muscles(o, "primaryMuscles"),
                    secondaryMuscles = muscles(o, "secondaryMuscles"),
                    trackedMetric = TrackedMetric.valueOf(o.getString("trackedMetric")),
                    instructions = strings(o, "instructions"),
                    isCustom = false,
                )
            }.getOrNull()?.let { entities.add(it) }
        }
        if (entities.isNotEmpty()) dao.upsertAll(entities)
    }

    private fun muscles(o: JSONObject, key: String): List<MuscleGroup> =
        strings(o, key).mapNotNull { runCatching { MuscleGroup.valueOf(it) }.getOrNull() }

    /** Tolerates a JSON array, a single string, or a missing field (PowerShell can emit any of these). */
    private fun strings(o: JSONObject, key: String): List<String> =
        when (val v = o.opt(key)) {
            is JSONArray -> (0 until v.length()).map { v.optString(it) }.filter { it.isNotBlank() }
            is String -> if (v.isBlank()) emptyList() else listOf(v)
            else -> emptyList()
        }
}
