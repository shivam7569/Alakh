package com.andy.alakh.health

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.andy.alakh.shared.model.WorkoutType

/**
 * Single entry point for starting/stopping live sensor tracking. Both goes through [ExerciseService]
 * so Health Services keeps delivering HR/calories in a foreground service that survives screen-off.
 */
object WorkoutSensors {

    /** Must be called while the app is foreground (BODY_SENSORS is while-in-use). */
    fun start(context: Context, type: WorkoutType = WorkoutType.OTHER) {
        val intent = Intent(context, ExerciseService::class.java).apply {
            action = ExerciseService.ACTION_START
            putExtra(ExerciseService.EXTRA_TYPE, type.name)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, ExerciseService::class.java).apply {
            action = ExerciseService.ACTION_STOP
        }
        context.startService(intent)
    }
}
