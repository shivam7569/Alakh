package com.andy.alakh.presentation.workout

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.andy.alakh.health.ExerciseRepository
import com.andy.alakh.health.ExerciseService
import com.andy.alakh.shared.model.WorkoutType

/**
 * Bridges the UI and the workout pipeline. Live metrics come straight from the
 * (process-singleton) repository; start/stop go through [ExerciseService] so the
 * exercise runs in a foreground service that survives screen-off.
 */
class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ExerciseRepository.getInstance(app)

    val metrics = repository.metrics
    val status = repository.status

    fun start(type: WorkoutType = WorkoutType.WALKING) {
        val intent = Intent(getApplication(), ExerciseService::class.java).apply {
            action = ExerciseService.ACTION_START
            putExtra(ExerciseService.EXTRA_TYPE, type.name)
        }
        // Must be started while the app is in the foreground (BODY_SENSORS is while-in-use).
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun stop() {
        val intent = Intent(getApplication(), ExerciseService::class.java).apply {
            action = ExerciseService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }
}
