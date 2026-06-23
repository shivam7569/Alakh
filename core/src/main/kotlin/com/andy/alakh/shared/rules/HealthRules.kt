package com.andy.alakh.shared.rules

/** Pure, framework-free business rules reused by the watch and (future) phone app. */
object HealthRules {

    /** Classic 5-zone model based on % of estimated max heart rate. */
    enum class HeartRateZone { REST, ZONE_1, ZONE_2, ZONE_3, ZONE_4, ZONE_5 }

    /** HRmax estimate (220 - age), floored so the math stays sane for odd inputs. */
    fun maxHeartRate(age: Int): Int = (220 - age).coerceAtLeast(100)

    /**
     * Whole-years age on a given date, from a birth date. Computed dynamically (not stored) so HR
     * zones stay correct as time passes. Pure: the caller passes "today" (e.g. from LocalDate.now()).
     */
    fun ageOnDate(
        birthYear: Int, birthMonth: Int, birthDay: Int,
        onYear: Int, onMonth: Int, onDay: Int,
    ): Int {
        var age = onYear - birthYear
        if (onMonth < birthMonth || (onMonth == birthMonth && onDay < birthDay)) age--
        return age.coerceAtLeast(0)
    }

    /** Zone from an explicit max HR — lets the watch use a manual override instead of 220−age. */
    fun zoneForMaxHeartRate(bpm: Int, maxHr: Int): HeartRateZone {
        val pct = bpm.toDouble() / maxHr.coerceAtLeast(1)
        return when {
            pct < 0.50 -> HeartRateZone.REST
            pct < 0.60 -> HeartRateZone.ZONE_1
            pct < 0.70 -> HeartRateZone.ZONE_2
            pct < 0.80 -> HeartRateZone.ZONE_3
            pct < 0.90 -> HeartRateZone.ZONE_4
            else -> HeartRateZone.ZONE_5
        }
    }

    /** % of max HR, clamped to 0–100 — drives the zone ring on the workout monitor. */
    fun heartRatePercent(bpm: Int, maxHr: Int): Int =
        ((bpm.toDouble() / maxHr.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)

    fun heartRateZone(bpm: Int, age: Int): HeartRateZone = zoneForMaxHeartRate(bpm, maxHeartRate(age))

    /** A guided breathing pattern, in seconds per phase. */
    data class BreathingPattern(
        val inhaleSec: Int = 4,
        val holdSec: Int = 4,
        val exhaleSec: Int = 4,
        val holdAfterExhaleSec: Int = 4,
    ) {
        val cycleSec: Int get() = inhaleSec + holdSec + exhaleSec + holdAfterExhaleSec
    }

    /** Box breathing: 4-4-4-4. */
    val BOX_BREATHING = BreathingPattern()

    /** Relaxing 4-7-8 pattern. */
    val RELAXING_478 = BreathingPattern(inhaleSec = 4, holdSec = 7, exhaleSec = 8, holdAfterExhaleSec = 0)
}
