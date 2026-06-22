package com.andy.alakh.shared.rules

import com.andy.alakh.shared.rules.HealthRules.HeartRateZone
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HealthRulesTest {

    @Test
    fun maxHeartRate_usesAgeFormula() {
        assertThat(HealthRules.maxHeartRate(30)).isEqualTo(190)
        assertThat(HealthRules.maxHeartRate(20)).isEqualTo(200)
    }

    @Test
    fun maxHeartRate_flooredAt100() {
        assertThat(HealthRules.maxHeartRate(130)).isEqualTo(100)
        assertThat(HealthRules.maxHeartRate(200)).isEqualTo(100)
    }

    @Test
    fun heartRateZone_classifiesEachBand() {
        // age 20 -> max HR 200, so bpm = pct * 200
        assertThat(HealthRules.heartRateZone(80, 20)).isEqualTo(HeartRateZone.REST)    // 0.40
        assertThat(HealthRules.heartRateZone(110, 20)).isEqualTo(HeartRateZone.ZONE_1) // 0.55
        assertThat(HealthRules.heartRateZone(130, 20)).isEqualTo(HeartRateZone.ZONE_2) // 0.65
        assertThat(HealthRules.heartRateZone(150, 20)).isEqualTo(HeartRateZone.ZONE_3) // 0.75
        assertThat(HealthRules.heartRateZone(170, 20)).isEqualTo(HeartRateZone.ZONE_4) // 0.85
        assertThat(HealthRules.heartRateZone(190, 20)).isEqualTo(HeartRateZone.ZONE_5) // 0.95
    }

    @Test
    fun heartRateZone_boundaryIsExclusiveUpperBound() {
        // exactly 60% of max (120 of 200) should land in ZONE_2, not ZONE_1
        assertThat(HealthRules.heartRateZone(120, 20)).isEqualTo(HeartRateZone.ZONE_2)
    }

    @Test
    fun heartRateZone_eachBandLowerBoundIsInclusive() {
        // age 20 -> max HR 200. Each threshold % should land in the HIGHER zone.
        assertThat(HealthRules.heartRateZone(100, 20)).isEqualTo(HeartRateZone.ZONE_1) // exactly 0.50
        assertThat(HealthRules.heartRateZone(120, 20)).isEqualTo(HeartRateZone.ZONE_2) // exactly 0.60
        assertThat(HealthRules.heartRateZone(140, 20)).isEqualTo(HeartRateZone.ZONE_3) // exactly 0.70
        assertThat(HealthRules.heartRateZone(160, 20)).isEqualTo(HeartRateZone.ZONE_4) // exactly 0.80
        assertThat(HealthRules.heartRateZone(180, 20)).isEqualTo(HeartRateZone.ZONE_5) // exactly 0.90
    }

    @Test
    fun heartRateZone_zeroOrVeryLowIsRest() {
        assertThat(HealthRules.heartRateZone(0, 20)).isEqualTo(HeartRateZone.REST)
        assertThat(HealthRules.heartRateZone(40, 20)).isEqualTo(HeartRateZone.REST)
    }

    @Test
    fun heartRateZone_aboveMaxStillClassifiesAsZone5() {
        assertThat(HealthRules.heartRateZone(210, 20)).isEqualTo(HeartRateZone.ZONE_5)
    }

    @Test
    fun maxHeartRate_handlesZeroAndNegativeAge() {
        assertThat(HealthRules.maxHeartRate(0)).isEqualTo(220)
        assertThat(HealthRules.maxHeartRate(-5)).isEqualTo(225) // formula still applies before the floor
    }

    @Test
    fun breathingPattern_cycleSeconds() {
        assertThat(HealthRules.BOX_BREATHING.cycleSec).isEqualTo(16)   // 4+4+4+4
        assertThat(HealthRules.RELAXING_478.cycleSec).isEqualTo(19)    // 4+7+8+0
    }

    @Test
    fun breathingPattern_defaultsToBox() {
        val p = HealthRules.BreathingPattern()
        assertThat(p.inhaleSec).isEqualTo(4)
        assertThat(p.holdSec).isEqualTo(4)
        assertThat(p.exhaleSec).isEqualTo(4)
        assertThat(p.holdAfterExhaleSec).isEqualTo(4)
    }
}
