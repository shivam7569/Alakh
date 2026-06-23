package com.andy.alakh.health

import android.content.Context
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.andy.alakh.shared.model.ExerciseMetrics
import com.andy.alakh.shared.model.ExerciseStatus
import com.andy.alakh.shared.model.WorkoutType
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Thin wrapper around the Health Services [ExerciseClient]. Owns the live
 * exercise state and exposes it as StateFlows the UI can observe.
 *
 * NOTE: This is the one file most worth double-checking against autocomplete for
 * your installed `health-services-client` version — the metric/state accessors
 * are the parts most likely to drift between releases. The structure (acquire
 * client -> set callback -> start/end) is stable.
 */
class ExerciseRepository private constructor(context: Context) {

    private val exerciseClient: ExerciseClient =
        HealthServices.getClient(context.applicationContext).exerciseClient

    private val _metrics = MutableStateFlow(ExerciseMetrics())
    val metrics: StateFlow<ExerciseMetrics> = _metrics.asStateFlow()

    private val _status = MutableStateFlow(ExerciseStatus.NOT_STARTED)
    val status: StateFlow<ExerciseStatus> = _status.asStateFlow()

    private val updateCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val hr = update.latestMetrics
                .getData(DataType.HEART_RATE_BPM)
                .lastOrNull()?.value?.toInt()
            // CALORIES_TOTAL / DISTANCE_TOTAL are cumulative aggregates (total since the exercise began).
            val calories = update.latestMetrics.getData(DataType.CALORIES_TOTAL)?.total
            val distance = update.latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total
            val elapsedMs = update.activeDurationCheckpoint?.activeDuration?.toMillis()
                ?: _metrics.value.elapsedMs

            _metrics.value = _metrics.value.copy(
                heartRateBpm = hr ?: _metrics.value.heartRateBpm,
                calories = calories ?: _metrics.value.calories,
                distanceMeters = distance ?: _metrics.value.distanceMeters,
                elapsedMs = elapsedMs,
            )

            _status.value = if (update.exerciseStateInfo.state == ExerciseState.ENDED) {
                ExerciseStatus.ENDED
            } else {
                ExerciseStatus.ACTIVE
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
        override fun onRegistered() {}
        override fun onRegistrationFailed(throwable: Throwable) {}
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
    }

    /** Begin an exercise. Caller must already hold BODY_SENSORS (and FINE_LOCATION if GPS). */
    suspend fun start(type: WorkoutType) {
        exerciseClient.setUpdateCallback(updateCallback)
        val config = ExerciseConfig.builder(type.toExerciseType())
            .setDataTypes(
                setOf(
                    DataType.HEART_RATE_BPM,
                    DataType.CALORIES_TOTAL,
                    DataType.DISTANCE_TOTAL,
                ),
            )
            .setIsAutoPauseAndResumeEnabled(false)
            .setIsGpsEnabled(false)
            .build()
        exerciseClient.startExerciseAsync(config).awaitFuture()
        _status.value = ExerciseStatus.ACTIVE
    }

    /** End the current exercise and detach the callback. Safe to call when idle. */
    suspend fun stop() {
        runCatching { exerciseClient.endExerciseAsync().awaitFuture() }
        runCatching { exerciseClient.clearUpdateCallbackAsync(updateCallback).awaitFuture() }
        _status.value = ExerciseStatus.ENDED
    }

    companion object {
        @Volatile private var instance: ExerciseRepository? = null

        fun getInstance(context: Context): ExerciseRepository =
            instance ?: synchronized(this) {
                instance ?: ExerciseRepository(context).also { instance = it }
            }
    }
}

private fun WorkoutType.toExerciseType(): ExerciseType = when (this) {
    WorkoutType.WALKING -> ExerciseType.WALKING
    WorkoutType.RUNNING -> ExerciseType.RUNNING
    WorkoutType.OUTDOOR_CYCLING -> ExerciseType.BIKING
    WorkoutType.HIKING -> ExerciseType.HIKING
    WorkoutType.OTHER -> ExerciseType.WORKOUT
}

/** Awaits a Guava ListenableFuture without pulling in kotlinx-coroutines-guava. */
private suspend fun <T> ListenableFuture<T>.awaitFuture(): T =
    suspendCancellableCoroutine { cont ->
        addListener(
            {
                try {
                    cont.resume(get())
                } catch (e: Throwable) {
                    cont.resumeWithException(e.cause ?: e)
                }
            },
            Executor { it.run() },
        )
        cont.invokeOnCancellation { cancel(false) }
    }
