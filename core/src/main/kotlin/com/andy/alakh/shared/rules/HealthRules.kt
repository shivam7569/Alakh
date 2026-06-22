package com.andy.alakh.shared.rules

/** Pure, framework-free business rules reused by the watch and (future) phone app. */
object HealthRules {

    /** Classic 5-zone model based on % of estimated max heart rate. */
    enum class HeartRateZone { REST, ZONE_1, ZONE_2, ZONE_3, ZONE_4, ZONE_5 }

    /** HRmax estimate (220 - age), floored so the math stays sane for odd inputs. */
    fun maxHeartRate(age: Int): Int = (220 - age).coerceAtLeast(100)

    fun heartRateZone(bpm: Int, age: Int): HeartRateZone {
        val pct = bpm.toDouble() / maxHeartRate(age)
        return when {
            pct < 0.50 -> HeartRateZone.REST
            pct < 0.60 -> HeartRateZone.ZONE_1
            pct < 0.70 -> HeartRateZone.ZONE_2
            pct < 0.80 -> HeartRateZone.ZONE_3
            pct < 0.90 -> HeartRateZone.ZONE_4
            else -> HeartRateZone.ZONE_5
        }
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
