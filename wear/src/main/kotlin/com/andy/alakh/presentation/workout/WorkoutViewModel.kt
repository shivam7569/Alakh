package com.andy.alakh.presentation.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.andy.alakh.health.ExerciseRepository

/**
 * Exposes the live workout metrics to the monitor screen. Start/stop of the Health Services exercise
 * is handled by [com.andy.alakh.health.WorkoutSensors] (the foreground service); this VM only reads
 * the process-singleton repository's flows.
 */
class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ExerciseRepository.getInstance(app)

    val metrics = repository.metrics
    val status = repository.status
}
