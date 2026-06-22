package com.andy.alakh.shared.model

/**
 * Muscle-group axes used everywhere muscle targeting matters: tagging exercises,
 * aggregating per-muscle volume, and drawing the post-workout spider (radar) chart.
 *
 * Kept to ~12 axes on purpose — enough detail to be meaningful, few enough to stay
 * readable on a radar chart (radar charts get unreadable much past ~8–12 axes). The
 * raw muscle tokens from the source dataset (e.g. "lats", "middle back") are mapped
 * onto these at catalog-seed time. [[wearos-health-app]]
 */
enum class MuscleGroup(val displayName: String, val onRadar: Boolean = true) {
    CHEST("Chest"),
    BACK("Back"),               // lats + mid/upper back
    SHOULDERS("Shoulders"),     // all three deltoid heads
    TRAPS("Traps"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    CORE("Core"),               // abs + obliques
    LOWER_BACK("Lower back"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    SHIN("Shins"),              // tibialis anterior (e.g. tib raises)
    // Catch-all for conditioning/cardio that doesn't map to one group; excluded from the radar.
    FULL_BODY("Full body", onRadar = false),
}
