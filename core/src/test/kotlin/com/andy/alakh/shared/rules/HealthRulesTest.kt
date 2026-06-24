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

    @Test
    fun ageOnDate_countsWholeYearsAndRespectsBirthday() {
        // DOB 1996-02-17.
        assertThat(HealthRules.ageOnDate(1996, 2, 17, 2026, 6, 23)).isEqualTo(30) // birthday passed
        assertThat(HealthRules.ageOnDate(1996, 2, 17, 2026, 2, 17)).isEqualTo(30) // on the birthday
        assertThat(HealthRules.ageOnDate(1996, 2, 17, 2026, 2, 16)).isEqualTo(29) // day before
        assertThat(HealthRules.ageOnDate(1996, 2, 17, 2026, 1, 5)).isEqualTo(29)  // earlier month
    }

    @Test
    fun zoneForMaxHeartRate_matchesBandsForExplicitMax() {
        // max 190 (age 30): zone thresholds at 95/114/133/152/171 bpm.
        assertThat(HealthRules.zoneForMaxHeartRate(90, 190)).isEqualTo(HeartRateZone.REST)
        assertThat(HealthRules.zoneForMaxHeartRate(95, 190)).isEqualTo(HeartRateZone.ZONE_1) // exactly 50%
        assertThat(HealthRules.zoneForMaxHeartRate(160, 190)).isEqualTo(HeartRateZone.ZONE_4)
        assertThat(HealthRules.zoneForMaxHeartRate(180, 190)).isEqualTo(HeartRateZone.ZONE_5)
    }

    @Test
    fun heartRatePercent_clampsToRange() {
        assertThat(HealthRules.heartRatePercent(95, 190)).isEqualTo(50)
        assertThat(HealthRules.heartRatePercent(0, 190)).isEqualTo(0)
        assertThat(HealthRules.heartRatePercent(250, 190)).isEqualTo(100) // clamped
    }

    @Test
    fun hrGaugePosition_normalHeartRateIsNotAtZero() {
        // Regression: 78 bpm at max 190 must land inside the Light zone, not the empty start.
        val p = HealthRules.hrGaugePosition(78, 190)
        assertThat(p.zoneIndex).isEqualTo(0)                 // Light
        assertThat(p.withinZone).isWithin(0.01f).of(0.622f)  // (78-50)/(95-50)
        assertThat(p.gaugeFraction).isGreaterThan(0.1f)
    }

    @Test
    fun hrGaugePosition_mapsEachZoneByBounds() {
        // max 190 → bounds 50 / 95 / 133 / 161.5 / 190.
        assertThat(HealthRules.hrGaugePosition(95, 190).zoneIndex).isEqualTo(1)  // exactly 50% → Fat burn start
        assertThat(HealthRules.hrGaugePosition(130, 190).zoneIndex).isEqualTo(1) // Fat burn
        assertThat(HealthRules.hrGaugePosition(150, 190).zoneIndex).isEqualTo(2) // Cardio
        assertThat(HealthRules.hrGaugePosition(175, 190).zoneIndex).isEqualTo(3) // Peak
    }

    @Test
    fun hrGaugePosition_clampsBelowFloorAndAboveMax() {
        val low = HealthRules.hrGaugePosition(40, 190)   // below the 50 floor
        assertThat(low.zoneIndex).isEqualTo(0)
        assertThat(low.withinZone).isEqualTo(0f)

        val high = HealthRules.hrGaugePosition(250, 190) // above max
        assertThat(high.zoneIndex).isEqualTo(3)
        assertThat(high.withinZone).isEqualTo(1f)
        assertThat(high.gaugeFraction).isEqualTo(1f)
    }

    @Test
    fun hrGaugePosition_handlesLowMaxWithoutDivideByZero() {
        // Floored max (100): bounds stay strictly increasing (floor capped below 50%).
        val p = HealthRules.hrGaugePosition(60, 100)
        assertThat(p.zoneIndex).isIn(0..3)
        assertThat(p.withinZone).isAtLeast(0f)
        assertThat(p.withinZone).isAtMost(1f)
    }
}
