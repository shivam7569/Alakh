package com.andy.alakh.health

import android.content.Context
import com.andy.alakh.shared.rules.HealthRules
import java.time.LocalDate

/**
 * The single user's profile (this is a personal app), persisted in SharedPreferences. Stores the
 * date of birth and an optional manual max-HR override; age is computed dynamically so HR zones stay
 * correct as time passes. Seeded to the owner's DOB (1996-02-17). A future settings screen can edit
 * the DOB or set [setManualMaxHr] to configure HR zones manually.
 */
object UserProfile {
    private const val PREFS = "alakh_profile"
    private const val KEY_DOB_EPOCH_DAY = "dob_epoch_day"
    private const val KEY_MANUAL_MAX_HR = "manual_max_hr"
    private const val KEY_REST_SEC = "rest_duration_sec"
    private val DEFAULT_DOB: LocalDate = LocalDate.of(1996, 2, 17)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun dateOfBirth(context: Context): LocalDate {
        val epochDay = prefs(context).getLong(KEY_DOB_EPOCH_DAY, Long.MIN_VALUE)
        return if (epochDay == Long.MIN_VALUE) DEFAULT_DOB else LocalDate.ofEpochDay(epochDay)
    }

    fun setDateOfBirth(context: Context, date: LocalDate) {
        prefs(context).edit().putLong(KEY_DOB_EPOCH_DAY, date.toEpochDay()).apply()
    }

    /** Manual max-HR override; 0 = derive from age. */
    fun manualMaxHr(context: Context): Int = prefs(context).getInt(KEY_MANUAL_MAX_HR, 0)

    fun setManualMaxHr(context: Context, maxHr: Int) {
        prefs(context).edit().putInt(KEY_MANUAL_MAX_HR, maxHr.coerceAtLeast(0)).apply()
    }

    fun currentAge(context: Context, today: LocalDate = LocalDate.now()): Int {
        val dob = dateOfBirth(context)
        return HealthRules.ageOnDate(
            dob.year, dob.monthValue, dob.dayOfMonth,
            today.year, today.monthValue, today.dayOfMonth,
        )
    }

    /** The manual override if set, otherwise 220 − age. Drives the workout monitor's HR zones. */
    fun maxHeartRate(context: Context): Int {
        val manual = manualMaxHr(context)
        return if (manual > 0) manual else HealthRules.maxHeartRate(currentAge(context))
    }

    /** Preferred rest length between sets (seconds), default 90; adjusted from the rest banner. */
    fun restDurationSec(context: Context): Int = prefs(context).getInt(KEY_REST_SEC, 90)

    fun setRestDurationSec(context: Context, seconds: Int) {
        prefs(context).edit().putInt(KEY_REST_SEC, seconds.coerceIn(15, 600)).apply()
    }
}
