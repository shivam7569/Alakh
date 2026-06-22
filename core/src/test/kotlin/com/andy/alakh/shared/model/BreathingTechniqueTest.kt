package com.andy.alakh.shared.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BreathingTechniqueTest {

    private fun technique(
        inhale: Double = 4.0, hold: Double = 4.0, exhale: Double = 4.0, holdAfter: Double = 4.0,
        safety: BreathSafetyLevel = BreathSafetyLevel.GENERAL,
    ) = BreathingTechnique(
        id = "t", name = "T", category = BreathCategory.RELAXATION, patternType = BreathPatternType.PACED,
        inhaleSec = inhale, holdSec = hold, exhaleSec = exhale, holdAfterExhaleSec = holdAfter,
        summary = "", howItWorks = "", difficulty = BreathDifficulty.BEGINNER, safetyLevel = safety,
    )

    @Test
    fun cycleSec_sumsTheFourPhases() {
        assertThat(technique(4.0, 4.0, 4.0, 4.0).cycleSec).isEqualTo(16.0)   // box
        assertThat(technique(4.0, 7.0, 8.0, 0.0).cycleSec).isEqualTo(19.0)   // 4-7-8
    }

    @Test
    fun cycleSec_supportsFractionalSeconds() {
        // resonant / coherent breathing: 5.5s in, 5.5s out, no holds.
        assertThat(technique(5.5, 0.0, 5.5, 0.0).cycleSec).isWithin(1e-9).of(11.0)
    }

    @Test
    fun requiresAcknowledgement_onlyForNonGeneralTiers() {
        assertThat(technique(safety = BreathSafetyLevel.GENERAL).requiresAcknowledgement).isFalse()
        assertThat(technique(safety = BreathSafetyLevel.CAUTION).requiresAcknowledgement).isTrue()
        assertThat(technique(safety = BreathSafetyLevel.ADVANCED).requiresAcknowledgement).isTrue()
    }
}
