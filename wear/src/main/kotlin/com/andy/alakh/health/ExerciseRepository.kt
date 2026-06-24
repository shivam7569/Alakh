package com.andy.alakh.health

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
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

    private val appContext = context.applicationContext
    private val exerciseClient: ExerciseClient =
        HealthServices.getClient(appContext).exerciseClient

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private val _metrics = MutableStateFlow(ExerciseMetrics())
    val metrics: StateFlow<ExerciseMetrics> = _metrics.asStateFlow()

    private val _status = MutableStateFlow(ExerciseStatus.NOT_STARTED)
    val status: StateFlow<ExerciseStatus> = _status.asStateFlow()

    /** Human-readable reason HR isn't showing (start error, off-body, acquiring…). null = fine. */
    private val _diagnostic = MutableStateFlow<String?>(null)
    val diagnostic: StateFlow<String?> = _diagnostic.asStateFlow()

    private val updateCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val hr = update.latestMetrics
                .getData(DataType.HEART_RATE_BPM)
                .lastOrNull()?.value?.toInt()
            if (hr != null) _diagnostic.value = null
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
        override fun onRegistrationFailed(throwable: Throwable) {
            Log.w(TAG, "Sensor registration failed", throwable)
            _diagnostic.value = "Sensor connect failed"
        }

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            if (dataType == DataType.HEART_RATE_BPM && availability is DataTypeAvailability) {
                _diagnostic.value = when (availability) {
                    DataTypeAvailability.AVAILABLE -> null
                    DataTypeAvailability.ACQUIRING -> "Acquiring heart rate…"
                    DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY -> "Wear watch snugly"
                    else -> "HR sensor unavailable"
                }
            }
        }
    }

    /**
     * Begin an exercise. Caller must already hold BODY_SENSORS. Only the data types the device
     * actually supports for this exercise are requested — asking for an unsupported one (e.g.
     * distance for a stationary strength workout) makes Health Services reject the whole start.
     */
    suspend fun start(type: WorkoutType) {
        try {
            _status.value = ExerciseStatus.PREPARING
            _diagnostic.value = "Starting…"
            exerciseClient.setUpdateCallback(updateCallback)

            val exerciseType = type.toExerciseType()
            val supported = exerciseClient.getCapabilitiesAsync().awaitFuture()
                .getExerciseTypeCapabilities(exerciseType)
                .supportedDataTypes

            // Only request a metric when the device supports it AND we hold its permission, so a
            // missing calories permission can't block heart rate. (HR = BODY_SENSORS; calories =
            // ACTIVITY_RECOGNITION.) Distance is irrelevant to a stationary strength workout.
            val wantsHr = DataType.HEART_RATE_BPM in supported && granted(Manifest.permission.BODY_SENSORS)
            val wantsCalories = DataType.CALORIES_TOTAL in supported && granted(Manifest.permission.ACTIVITY_RECOGNITION)
            val wanted = mutableSetOf<DataType<*, *>>()
            if (wantsHr) wanted.add(DataType.HEART_RATE_BPM)
            if (wantsCalories) wanted.add(DataType.CALORIES_TOTAL)

            val config = ExerciseConfig.builder(exerciseType)
                .setDataTypes(wanted)
                .setIsAutoPauseAndResumeEnabled(false)
                .setIsGpsEnabled(false)
                .build()
            exerciseClient.startExerciseAsync(config).awaitFuture()
            _status.value = ExerciseStatus.ACTIVE
            _diagnostic.value = if (wantsHr) "Acquiring heart rate…" else "Heart rate off"
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to start exercise", e)
            _status.value = ExerciseStatus.NOT_STARTED
            _diagnostic.value = "Couldn't start tracking"
        }
    }

    /** End the current exercise and detach the callback. Safe to call when idle. */
    suspend fun stop() {
        runCatching { exerciseClient.endExerciseAsync().awaitFuture() }
        runCatching { exerciseClient.clearUpdateCallbackAsync(updateCallback).awaitFuture() }
        _status.value = ExerciseStatus.ENDED
    }

    companion object {
        private const val TAG = "ExerciseRepository"

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
