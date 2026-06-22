package com.andy.alakh.presentation.breathing

import com.andy.alakh.shared.model.BreathingTechnique

/**
 * Carries the chosen technique between the catalog, detail, and run screens without nav arguments
 * (same lightweight pattern as CatalogNav), and remembers which gated techniques the user has
 * acknowledged for the life of the app process so the safety gate isn't nagged every single time.
 */
object BreathingNav {
    var selected: BreathingTechnique? = null

    private val acknowledged = mutableSetOf<String>()
    fun isAcknowledged(id: String): Boolean = id in acknowledged
    fun acknowledge(id: String) { acknowledged += id }
}
