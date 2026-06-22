package com.andy.alakh.shared.model

/**
 * How a guided breath is structured.
 * - [PACED]: a fixed inhale / hold / exhale / hold-after rhythm repeated for the session (most techniques).
 * - [ROUNDS]: repeated rounds of fast "power breaths" then a breath-hold/retention (e.g. Wim Hof).
 * - [FREEFORM]: rapid continuous breathing with no per-breath hold (e.g. Kapalabhati).
 */
enum class BreathPatternType { PACED, ROUNDS, FREEFORM }

/** Browse buckets for the catalog. */
enum class BreathCategory(val displayName: String) {
    RELAXATION("Relaxation"),
    SLEEP("Sleep"),
    FOCUS("Focus"),
    ENERGIZING("Energizing"),
    PERFORMANCE("Performance"),
    PRANAYAMA("Pranayama"),
    CLINICAL("Therapeutic"),
    EVERYDAY("Everyday"),
}

enum class BreathDifficulty { BEGINNER, INTERMEDIATE, ADVANCED }

/**
 * Safety tier that drives gating:
 * - [GENERAL]: fine for almost anyone.
 * - [CAUTION]: breath retention / strongly extended exhale that some people should avoid.
 * - [ADVANCED]: hyperventilation / strong retention with real medical contraindications.
 *
 * CAUTION and ADVANCED techniques are shown behind a one-time acknowledgement before they can start.
 */
enum class BreathSafetyLevel { GENERAL, CAUTION, ADVANCED }

/**
 * One catalog breathing technique. Pure, framework-free domain data (lives in :core so it's
 * unit-tested on CI and reusable by the phone app). Phase durations are in SECONDS as Doubles so
 * patterns like resonant breathing (5.5s in / 5.5s out) are represented faithfully; a phase that
 * doesn't apply is 0.0. The actual catalog content is in [com.andy.alakh.shared.rules.BreathingCatalog].
 */
data class BreathingTechnique(
    val id: String,
    val name: String,
    val aka: List<String> = emptyList(),
    val category: BreathCategory,
    val patternType: BreathPatternType,
    val inhaleSec: Double,
    val holdSec: Double,
    val exhaleSec: Double,
    val holdAfterExhaleSec: Double,
    val defaultRounds: Int = 0,        // 0 = not round-based
    val defaultDurationSec: Int = 0,   // suggested total session length
    val summary: String,
    val howItWorks: String,
    val benefits: List<String> = emptyList(),
    val difficulty: BreathDifficulty,
    val safetyLevel: BreathSafetyLevel,
    val contraindications: List<String> = emptyList(),
    val safetyNote: String = "",
    val nasalNote: String = "",
    val sources: List<String> = emptyList(),
) {
    /** Seconds for one full paced cycle (inhale + hold + exhale + hold-after). */
    val cycleSec: Double get() = inhaleSec + holdSec + exhaleSec + holdAfterExhaleSec

    /** CAUTION / ADVANCED techniques must show a one-time acknowledgement before starting. */
    val requiresAcknowledgement: Boolean get() = safetyLevel != BreathSafetyLevel.GENERAL
}
