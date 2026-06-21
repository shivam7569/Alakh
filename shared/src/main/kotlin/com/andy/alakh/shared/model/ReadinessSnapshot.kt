package com.andy.alakh.shared.model

/**
 * Inputs + result for Alakh's OWN readiness score (phone-only, independent of Google's).
 *
 * The exact formula is being researched separately (HRV/RHR/sleep normalization vs a
 * personal rolling baseline, plus our own training load). This type just reserves the
 * shape so the data model, storage, and sync are ready when we implement the algorithm.
 */
data class ReadinessSnapshot(
    val epochDay: Long,
    val hrvRmssd: Double? = null,          // overnight HRV (read from Health Connect on the phone)
    val restingHeartRate: Double? = null,
    val sleepScore: Double? = null,        // exact unit TBD by the formula research
    val trainingLoadAcute: Double? = null, // our edge: from our own logged sessions
    val trainingLoadChronic: Double? = null,
    val score: Int? = null,                // final 0–100, null until computed
    val baselineWindowDays: Int = 21,
)
