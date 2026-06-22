package com.andy.alakh.shared.rules

import com.andy.alakh.shared.model.BreathCategory
import com.andy.alakh.shared.model.BreathPatternType
import com.andy.alakh.shared.model.BreathSafetyLevel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Integrity tests for the generated breathing catalog. These guard the *data quality* of
 * [BreathingCatalog] on CI: unique ids, valid timing, and — most importantly — that the safety
 * gating is internally consistent (no retention technique slips through as GENERAL with no warning).
 */
class BreathingCatalogTest {

    private val all = BreathingCatalog.all

    @Test
    fun catalogIsExhaustiveAndNonEmpty() {
        assertThat(all.size).isAtLeast(40)
    }

    @Test
    fun everyIdIsUnique() {
        val ids = all.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun coreTextFieldsAreNeverBlank() {
        all.forEach { t ->
            assertThat(t.id).isNotEmpty()
            assertThat(t.name).isNotEmpty()
            assertThat(t.summary).isNotEmpty()
            assertThat(t.howItWorks).isNotEmpty()
            assertThat(t.safetyNote).isNotEmpty()
            assertThat(t.nasalNote).isNotEmpty()
            assertThat(t.benefits).isNotEmpty()
        }
    }

    @Test
    fun pacedTechniquesHaveAPositiveCycle() {
        // A paced (metronome) technique with an all-zero pattern would render a blank animation.
        all.filter { it.patternType == BreathPatternType.PACED }.forEach { t ->
            assertThat(t.cycleSec).isGreaterThan(0.0)
        }
    }

    @Test
    fun acknowledgementGateMatchesSafetyTier() {
        all.forEach { t ->
            val expected = t.safetyLevel != BreathSafetyLevel.GENERAL
            assertThat(t.requiresAcknowledgement).isEqualTo(expected)
        }
    }

    @Test
    fun cautionAndAdvancedTechniquesCarrySafetyContent() {
        // Anything gated MUST tell the user why — non-empty contraindications + a safety note.
        all.filter { it.safetyLevel != BreathSafetyLevel.GENERAL }.forEach { t ->
            assertThat(t.contraindications).isNotEmpty()
            assertThat(t.safetyNote).isNotEmpty()
        }
    }

    @Test
    fun allThreeSafetyTiersArePresent() {
        val tiers = all.map { it.safetyLevel }.toSet()
        assertThat(tiers).containsExactly(
            BreathSafetyLevel.GENERAL, BreathSafetyLevel.CAUTION, BreathSafetyLevel.ADVANCED,
        )
    }

    @Test
    fun orderedSafeFirstRiskiestLast() {
        assertThat(all.first().safetyLevel).isEqualTo(BreathSafetyLevel.GENERAL)
        assertThat(all.last().safetyLevel).isEqualTo(BreathSafetyLevel.ADVANCED)
    }

    @Test
    fun retentionWarningShowsForHoldsAndHyperventilation() {
        val box = BreathingCatalog.byId("box-breathing")!!          // has holds
        val kapalabhati = BreathingCatalog.byId("kapalabhati")!!    // FREEFORM hyperventilation
        val slow = BreathingCatalog.byId("slow-paced-breathing")!!  // GENERAL, no holds
        assertThat(BreathingCatalog.showsRetentionWarning(box)).isTrue()
        assertThat(BreathingCatalog.showsRetentionWarning(kapalabhati)).isTrue()
        assertThat(BreathingCatalog.showsRetentionWarning(slow)).isFalse()
    }

    @Test
    fun byId_findsKnownAndRejectsUnknown() {
        assertThat(BreathingCatalog.byId("box-breathing")).isNotNull()
        assertThat(BreathingCatalog.byId("not-a-real-id")).isNull()
    }

    @Test
    fun byCategory_isConsistentWithEntries() {
        BreathCategory.entries.forEach { c ->
            assertThat(BreathingCatalog.byCategory(c)).isEqualTo(all.filter { it.category == c })
        }
    }

    @Test
    fun spotChecks_knownTechniquesHaveExpectedShape() {
        val box = BreathingCatalog.byId("box-breathing")!!
        assertThat(box.cycleSec).isEqualTo(16.0)
        assertThat(box.safetyLevel).isEqualTo(BreathSafetyLevel.CAUTION) // brief holds → gated

        val coherent = BreathingCatalog.byId("coherent-resonance-breathing")!!
        assertThat(coherent.inhaleSec).isEqualTo(5.5)
        assertThat(coherent.exhaleSec).isEqualTo(5.5)
        assertThat(coherent.safetyLevel).isEqualTo(BreathSafetyLevel.GENERAL)

        val wimHof = BreathingCatalog.byId("wim-hof-method")!!
        assertThat(wimHof.patternType).isEqualTo(BreathPatternType.ROUNDS)
        assertThat(wimHof.safetyLevel).isEqualTo(BreathSafetyLevel.ADVANCED)
    }

    @Test
    fun gatingConstantsArePresent() {
        assertThat(BreathingCatalog.GLOBAL_DISCLAIMER).contains("not medical advice")
        assertThat(BreathingCatalog.RETENTION_WARNING).contains("water")
    }
}
