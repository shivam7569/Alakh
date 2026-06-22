package com.andy.alakh.shared.model

/**
 * A completed (or partial) guided breathing session. Logged INDEPENDENTLY of workouts —
 * the user does one or the other at a time. techniqueId is a stable key like "resonance"
 * or "physiological_sigh".
 */
data class BreathingSessionLog(
    val id: String,
    val techniqueId: String,
    val startedAtEpochMs: Long,
    val durationSec: Int,
    val completedCycles: Int,
)
