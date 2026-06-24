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

    /**
     * Where a heart rate sits on the workout monitor's four-segment gauge (Light / Fat-burn / Cardio
     * / Peak): [zoneIndex] 0–3, the [withinZone] fraction 0–1, and the overall [gaugeFraction] 0–1.
     * Zone bounds are a resting floor, then 50 / 70 / 85% of max HR — the floor keeps a normal
     * resting/working HR off the empty start of the gauge.
     */
    data class HrGaugePosition(val zoneIndex: Int, val withinZone: Float, val gaugeFraction: Float)

    fun hrGaugePosition(bpm: Int, maxHr: Int, restingFloorBpm: Int = 50): HrGaugePosition {
        val safeMax = maxHr.coerceAtLeast(restingFloorBpm + 4)
        val floor = restingFloorBpm.toFloat().coerceAtMost(safeMax * 0.45f) // keep bounds strictly increasing
        val bounds = floatArrayOf(floor, safeMax * 0.50f, safeMax * 0.70f, safeMax * 0.85f, safeMax.toFloat())
        val v = bpm.toFloat().coerceIn(bounds[0], bounds[4])
        var zone = 3
        for (i in 0 until 4) if (v < bounds[i + 1]) { zone = i; break }
        val within = ((v - bounds[zone]) / (bounds[zone + 1] - bounds[zone])).coerceIn(0f, 1f)
        return HrGaugePosition(zone, within, (zone + within) / 4f)
    }

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
